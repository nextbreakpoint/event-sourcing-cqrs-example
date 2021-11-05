package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.AmpqTestTarget;
import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignCommand;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignCommand;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignCommand;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.Vertx;
import org.apache.http.HttpRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.hamcrest.Matchers.notNullValue;

public class PactTests {
    private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
    private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
    private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";

    private static final TestScenario scenario = new TestScenario();

    private static Environment environment = Environment.getDefaultEnvironment();

    private static final List<ConsumerRecord<String, String>> records = new ArrayList<>();
    private static KafkaConsumer<String, String> consumer;
    private static Thread polling;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.provider.version", scenario.getVersion());

        final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

        consumer = KafkaUtils.createConsumer(environment, scenario.createConsumerConfig("test"));

        consumer.subscribe(Collections.singleton("design-command"));

        polling = createConsumerThread();

        polling.start();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        if (polling != null) {
            try {
                polling.interrupt();
                polling.join();
            } catch (Exception ignore) {
            }
        }

        if (consumer != null) {
            try {
                consumer.close();
            } catch (Exception ignore) {
            }
        }

        scenario.after();
    }

    @Nested
    @Tag("slow")
    @Tag("pact")
    @DisplayName("Verify contract between designs-command-producer and designs-command-consumer")
    @Provider("designs-command-producer")
    @Consumer("designs-command-consumer")
    @PactBroker
    public class VerifyDesignsCommandConsumer {
        @BeforeEach
        public void before(PactVerificationContext context) {
            context.setTarget(new AmpqTestTarget());
        }

        @TestTemplate
        @ExtendWith(PactVerificationInvocationContextProvider.class)
        @DisplayName("Verify interaction")
        public void pactVerificationTestTemplate(PactVerificationContext context) {
        }

        @State("kafka topic exists")
        public void kafkaTopicExists() {
        }

        @PactVerifyProvider("command to insert design")
        public String verifyInsertDesign() throws MalformedURLException {
            final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

            pause(100);

            long timestamp = System.currentTimeMillis();

            safelyClearMessages();

            final String uuid = createDesign(authorization, createPostData(MANIFEST, METADATA, SCRIPT));

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(uuid);
                        assertThat(messages.isEmpty()).isFalse();
                        Message decodedMessage = messages.get(messages.size() - 1);
                        assertThat(decodedMessage.getType()).isEqualTo("design-insert");
                        assertThat(decodedMessage.getUuid()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isNotNull();
                        assertThat(decodedMessage.getSource()).isNotNull();
                        assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                        InsertDesignCommand decodedEvent = Json.decodeValue(decodedMessage.getBody(), InsertDesignCommand.class);
                        assertThat(decodedEvent.getUuid()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isEqualTo(decodedEvent.getUuid().toString());
                        Design decodedDesign = Json.decodeValue(decodedEvent.getJson(), Design.class);
                        assertThat(decodedDesign.getManifest()).isEqualTo(MANIFEST);
                        assertThat(decodedDesign.getMetadata()).isEqualTo(METADATA);
                        assertThat(decodedDesign.getScript()).isEqualTo(SCRIPT);
                    });

            final List<Message> messages = safelyFindMessages(uuid);
            assertThat(messages.isEmpty()).isFalse();
            Message decodedMessage = messages.get(messages.size() - 1);

            return Json.encode(decodedMessage);
        }

        @PactVerifyProvider("command to update design")
        public String verifyUpdateDesign() throws MalformedURLException {
            final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

            pause(100);

            final String uuid = UUID.randomUUID().toString();

            long timestamp = System.currentTimeMillis();

            safelyClearMessages();

            updateDesign(authorization, createPostData(MANIFEST, METADATA, SCRIPT), uuid);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(uuid);
                        assertThat(messages.isEmpty()).isFalse();
                        Message decodedMessage = messages.get(messages.size() - 1);
                        assertThat(decodedMessage.getType()).isEqualTo("design-update");
                        assertThat(decodedMessage.getUuid()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isEqualTo(uuid);
                        assertThat(decodedMessage.getSource()).isNotNull();
                        assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                        UpdateDesignCommand decodedEvent = Json.decodeValue(decodedMessage.getBody(), UpdateDesignCommand.class);
                        assertThat(decodedEvent.getUuid()).isNotNull();
                        assertThat(decodedEvent.getUuid().toString()).isEqualTo(uuid);
                        Design decodedDesign = Json.decodeValue(decodedEvent.getJson(), Design.class);
                        assertThat(decodedDesign.getManifest()).isEqualTo(MANIFEST);
                        assertThat(decodedDesign.getMetadata()).isEqualTo(METADATA);
                        assertThat(decodedDesign.getScript()).isEqualTo(SCRIPT);
                    });

            final List<Message> messages = safelyFindMessages(uuid);
            assertThat(messages.isEmpty()).isFalse();
            Message decodedMessage = messages.get(messages.size() - 1);

            return Json.encode(decodedMessage);
        }

        @PactVerifyProvider("command to delete design")
        public String verifyDeleteDesign() throws MalformedURLException {
            final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

            pause(100);

            final String uuid = UUID.randomUUID().toString();

            long timestamp = System.currentTimeMillis();

            safelyClearMessages();

            deleteDesign(authorization, uuid);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(uuid);
                        assertThat(messages.isEmpty()).isFalse();
                        Message decodedMessage = messages.get(messages.size() - 1);
                        assertThat(decodedMessage.getType()).isEqualTo("design-delete");
                        assertThat(decodedMessage.getUuid()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isEqualTo(uuid);
                        assertThat(decodedMessage.getSource()).isNotNull();
                        assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                        DeleteDesignCommand decodedEvent = Json.decodeValue(decodedMessage.getBody(), DeleteDesignCommand.class);
                        assertThat(decodedEvent.getUuid()).isNotNull();
                        assertThat(decodedEvent.getUuid().toString()).isEqualTo(uuid);
                    });

            final List<Message> messages = safelyFindMessages(uuid);
            assertThat(messages.isEmpty()).isFalse();
            Message decodedMessage = messages.get(messages.size() - 1);

            return Json.encode(decodedMessage);
        }
    }

    @Nested
    @Tag("slow")
    @Tag("pact")
    @DisplayName("Verify contract between designs-command-producer and frontend")
    @Provider("designs-command-producer")
    @Consumer("frontend")
    @PactBroker
    public class VerifyFrontend {
        @BeforeEach
        public void before(PactVerificationContext context) {
            context.setTarget(new HttpsTestTarget(scenario.getServiceHost(), Integer.parseInt(scenario.getServicePort()), "/", true));
        }

        @TestTemplate
        @ExtendWith(PactVerificationInvocationContextProvider.class)
        @DisplayName("Verify interaction")
        public void pactVerificationTestTemplate(PactVerificationContext context, HttpRequest request) {
            final String authorization = scenario.makeAuthorization(Authentication.NULL_USER_UUID, Authority.ADMIN);
            request.setHeader(Headers.AUTHORIZATION, authorization);
            context.verifyInteraction();
        }

        @State("kafka topic exists")
        public void kafkaTopicExists() {
        }
    }

    private static void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private static List<Message> safelyFindMessages(String designId) {
        synchronized (records) {
            return records.stream()
                    .map(record -> Json.decodeValue(record.value(), Message.class))
                    .filter(value -> value.getPartitionKey().equals(designId))
                    .collect(Collectors.toList());
        }
    }

    private static void safelyClearMessages() {
        synchronized (records) {
            records.clear();
        }
    }

    private static void safelyAppendRecord(ConsumerRecord<String, String> record) {
        synchronized (records) {
            records.add(record);
        }
    }

    private static Thread createConsumerThread() {
        return new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(5));
                    System.out.println("Received " + consumerRecords.count() + " messages");
                    consumerRecords.forEach(PactTests::safelyAppendRecord);
                    consumer.commitSync();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void deleteDesign(String authorization, String uuid) throws MalformedURLException {
        given().config(scenario.getRestAssuredConfig())
                .and().header(AUTHORIZATION, authorization)
                .and().accept(ContentType.JSON)
                .when().delete(scenario.makeBaseURL("/v1/designs/" + uuid))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
    }

    private static String createDesign(String authorization, Map<String, Object> design) throws MalformedURLException {
        return given().config(scenario.getRestAssuredConfig())
                .and().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(design)
                .when().post(scenario.makeBaseURL("/v1/designs"))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON)
                .and().body("uuid", notNullValue())
                .and().extract().response().body().jsonPath().getString("uuid");
    }

    private static void updateDesign(String authorization, Map<String, Object> design, String uuid) throws MalformedURLException {
        given().config(scenario.getRestAssuredConfig())
                .and().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(design)
                .when().put(scenario.makeBaseURL("/v1/designs/" + uuid))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
    }

    private static Map<String, Object> createPostData(String manifest, String metadata, String script) {
        final Map<String, Object> data = new HashMap<>();
        data.put("manifest", manifest);
        data.put("metadata", metadata);
        data.put("script", script);
        return data;
    }
}
