package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Design;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.command.DeleteDesign;
import com.nextbreakpoint.blueprint.common.core.command.InsertDesign;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import io.vertx.core.json.Json;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.*;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

@Tag("slow")
public class TestSuite {
    private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
    private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
    private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";

    private static final TestScenario scenario = new TestScenario();

    private static Environment environment = Environment.getDefaultEnvironment();

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        scenario.after();
    }

    @Nested
    @Tag("integration")
    @DisplayName("Verify behaviour of designs-command-producer service")
    public class VerifyServiceIntegration {
        @AfterEach
        public void reset() {
            RestAssured.reset();
        }

        @Test
        @DisplayName("Should allow OPTIONS on /v1/designs without access token")
        public void shouldAllowOptionsOnDesignsWithoutAccessToken() throws MalformedURLException {
            given().config(scenario.getRestAssuredConfig())
                    .with().header("Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
                    .when().options(scenario.makeBaseURL("/v1/designs"))
                    .then().assertThat().statusCode(204)
                    .and().header("Access-Control-Allow-Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
                    .and().header("Access-Control-Allow-Credentials", "true");
        }

        @Test
        @DisplayName("Should allow OPTIONS on /v1/designs/id without access token")
        public void shouldAllowOptionsOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
            given().config(scenario.getRestAssuredConfig())
                    .with().header("Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
                    .when().options(scenario.makeBaseURL("/v1/designs/" + UUID.randomUUID().toString()))
                    .then().assertThat().statusCode(204)
                    .and().header("Access-Control-Allow-Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
                    .and().header("Access-Control-Allow-Credentials", "true");
        }

        @Test
        @DisplayName("Should forbid POST on /v1/designs without access token")
        public void shouldForbidPostOnDesignsWithoutAccessToken() throws MalformedURLException {
            given().config(scenario.getRestAssuredConfig())
                    .and().contentType(ContentType.JSON)
                    .and().accept(ContentType.JSON)
                    .and().body(createPostData())
                    .when().post(scenario.makeBaseURL("/v1/designs"))
                    .then().assertThat().statusCode(403);
        }

        @Test
        @DisplayName("Should forbid PUT on /v1/designs/id without access token")
        public void shouldForbidPutOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
            given().config(scenario.getRestAssuredConfig())
                    .and().contentType(ContentType.JSON)
                    .and().accept(ContentType.JSON)
                    .and().body(createPostData())
                    .when().put(scenario.makeBaseURL("/v1/designs/" + UUID.randomUUID().toString()))
                    .then().assertThat().statusCode(403);
        }

        @Test
        @DisplayName("Should forbid DELETE on /v1/designs/id without access token")
        public void shouldForbidDeleteOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
            final String uuid = UUID.randomUUID().toString();

            given().config(scenario.getRestAssuredConfig())
                    .and().accept(ContentType.JSON)
                    .when().delete(scenario.makeBaseURL("/v1/designs/" + uuid))
                    .then().assertThat().statusCode(403);
        }

        @Test
        @DisplayName("Should send a message after accepting POST on /v1/designs")
        public void shouldAcceptPostOnDesigns() throws IOException, InterruptedException {
            final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

            pause(100);

            final KafkaConsumer<String, String>[] consumer = new KafkaConsumer[1];

            Thread polling = null;

            try {
                consumer[0] = KafkaUtils.createConsumer(environment, scenario.createConsumerConfig("test-insert"));

                consumer[0].subscribe(Collections.singleton("designs-events"));

                long timestamp = System.currentTimeMillis();

                final List<ConsumerRecord<String, String>> records = new ArrayList<>();

                polling = createConsumerThread(records, consumer[0]);

                polling.start();

                createDesign(authorization, createPostData());

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            final Optional<Message> message = safelyFindMessage(records, "design-insert");
                            assertThat(message.isEmpty()).isFalse();
                            Message decodedMessage = message.get();
                            assertThat(decodedMessage.getMessageType()).isEqualTo("design-insert");
                            assertThat(decodedMessage.getMessageId()).isNotNull();
                            assertThat(decodedMessage.getPartitionKey()).isNotNull();
                            assertThat(decodedMessage.getMessageSource()).isNotNull();
                            assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                            InsertDesign decodedEvent = Json.decodeValue(decodedMessage.getMessageBody(), InsertDesign.class);
                            assertThat(decodedEvent.getUuid()).isNotNull();
                            assertThat(decodedMessage.getPartitionKey()).isEqualTo(decodedEvent.getUuid().toString());
                            Design decodedDesign = Json.decodeValue(decodedEvent.getJson(), Design.class);
                            assertThat(decodedDesign.getManifest()).isEqualTo(MANIFEST);
                            assertThat(decodedDesign.getMetadata()).isEqualTo(METADATA);
                            assertThat(decodedDesign.getScript()).isEqualTo(SCRIPT);
                        });
            } finally {
                if (polling != null) {
                    polling.interrupt();
                    polling.join();
                }
                if (consumer[0] != null) {
                    consumer[0].close();
                }
            }
        }

        @Test
        @DisplayName("Should send a message after accepting PUT on /v1/designs/id")
        public void shouldAcceptPutOnDesignsSlashId() throws IOException, InterruptedException {
            final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

            pause(100);

            final KafkaConsumer<String, String>[] consumer = new KafkaConsumer[1];

            Thread polling = null;

            try {
                consumer[0] = KafkaUtils.createConsumer(environment, scenario.createConsumerConfig("test-update"));

                consumer[0].subscribe(Collections.singleton("designs-events"));

                final String uuid = UUID.randomUUID().toString();

                long timestamp = System.currentTimeMillis();

                final List<ConsumerRecord<String, String>> records = new ArrayList<>();

                polling = createConsumerThread(records, consumer[0]);

                polling.start();

                updateDesign(authorization, createPostData(), uuid);

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            final Optional<Message> message = safelyFindMessage(records, "design-update");
                            assertThat(message.isEmpty()).isFalse();
                            Message decodedMessage = message.get();
                            assertThat(decodedMessage.getMessageType()).isEqualTo("design-update");
                            assertThat(decodedMessage.getMessageId()).isNotNull();
                            assertThat(decodedMessage.getPartitionKey()).isEqualTo(uuid);
                            assertThat(decodedMessage.getMessageSource()).isNotNull();
                            assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                            UpdateDesign decodedEvent = Json.decodeValue(decodedMessage.getMessageBody(), UpdateDesign.class);
                            assertThat(decodedEvent.getUuid()).isNotNull();
                            assertThat(decodedEvent.getUuid().toString()).isEqualTo(uuid);
                            Design decodedDesign = Json.decodeValue(decodedEvent.getJson(), Design.class);
                            assertThat(decodedDesign.getManifest()).isEqualTo(MANIFEST);
                            assertThat(decodedDesign.getMetadata()).isEqualTo(METADATA);
                            assertThat(decodedDesign.getScript()).isEqualTo(SCRIPT);
                        });
            } finally {
                if (polling != null) {
                    polling.interrupt();
                    polling.join();
                }
                if (consumer[0] != null) {
                    consumer[0].close();
                }
            }
        }

        @Test
        @DisplayName("Should send a message after accepting DELETE on /v1/designs/id")
        public void shouldAcceptDeleteOnDesignsSlashId() throws IOException, InterruptedException {
            final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

            pause(100);

            final KafkaConsumer<String, String>[] consumer = new KafkaConsumer[1];

            Thread polling = null;

            try {
                consumer[0] = KafkaUtils.createConsumer(environment, scenario.createConsumerConfig("test-delete"));

                consumer[0].subscribe(Collections.singleton("designs-events"));

                final String uuid = UUID.randomUUID().toString();

                long timestamp = System.currentTimeMillis();

                final List<ConsumerRecord<String, String>> records = new ArrayList<>();

                polling = createConsumerThread(records, consumer[0]);

                polling.start();

                deleteDesign(authorization, uuid);

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            final Optional<Message> message = safelyFindMessage(records, "design-delete");
                            assertThat(message.isEmpty()).isFalse();
                            Message decodedMessage = message.get();
                            assertThat(decodedMessage.getMessageType()).isEqualTo("design-delete");
                            assertThat(decodedMessage.getMessageId()).isNotNull();
                            assertThat(decodedMessage.getPartitionKey()).isEqualTo(uuid);
                            assertThat(decodedMessage.getMessageSource()).isNotNull();
                            assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                            DeleteDesign decodedEvent = Json.decodeValue(decodedMessage.getMessageBody(), DeleteDesign.class);
                            assertThat(decodedEvent.getUuid()).isNotNull();
                            assertThat(decodedEvent.getUuid().toString()).isEqualTo(uuid);
                        });
            } finally {
                if (polling != null) {
                    polling.interrupt();
                    polling.join();
                }
                if (consumer[0] != null) {
                    consumer[0].close();
                }
            }
        }

        private Optional<Message> safelyFindMessage(List<ConsumerRecord<String, String>> records, String messageType) {
            synchronized (records) {
                return records.stream()
                        .map(record -> Json.decodeValue(record.value(), Message.class))
                        .filter(message -> message.getMessageType().equals(messageType))
                        .findFirst();
            }
        }

        private void safelyAppendRecord(List<ConsumerRecord<String, String>> records, ConsumerRecord<String, String> record) {
            synchronized (records) {
                records.add(record);
            }
        }

        private Thread createConsumerThread(List<ConsumerRecord<String, String>> records, KafkaConsumer<String, String> kafkaConsumer) {
            return new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(5));
                        System.out.println("Received " + consumerRecords.count() + " messages");
                        consumerRecords.forEach(consumerRecord -> safelyAppendRecord(records, consumerRecord));
                        kafkaConsumer.commitSync();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        private void pause(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ignored) {
            }
        }

        private void deleteDesign(String authorization, String uuid) throws MalformedURLException {
            given().config(scenario.getRestAssuredConfig())
                    .and().header(AUTHORIZATION, authorization)
                    .and().accept(ContentType.JSON)
                    .when().delete(scenario.makeBaseURL("/v1/designs/" + uuid))
                    .then().assertThat().statusCode(202)
                    .and().contentType(ContentType.JSON);
        }

        private void createDesign(String authorization, Map<String, Object> design) throws MalformedURLException {
            given().config(scenario.getRestAssuredConfig())
                    .and().header(AUTHORIZATION, authorization)
                    .and().contentType(ContentType.JSON)
                    .and().accept(ContentType.JSON)
                    .and().body(design)
                    .when().post(scenario.makeBaseURL("/v1/designs"))
                    .then().assertThat().statusCode(202)
                    .and().contentType(ContentType.JSON);
//            .and().body("messageId", notNullValue())
//            .and().extract().response().body().jsonPath().getString("messageId");
        }

        private void updateDesign(String authorization, Map<String, Object> design, String uuid) throws MalformedURLException {
            given().config(scenario.getRestAssuredConfig())
                    .and().header(AUTHORIZATION, authorization)
                    .and().contentType(ContentType.JSON)
                    .and().accept(ContentType.JSON)
                    .and().body(design)
                    .when().put(scenario.makeBaseURL("/v1/designs/" + uuid))
                    .then().assertThat().statusCode(202)
                    .and().contentType(ContentType.JSON);
        }

        private Map<String, Object> createPostData() {
            final Map<String, Object> data = new HashMap<>();
            data.put("manifest", MANIFEST);
            data.put("metadata", METADATA);
            data.put("script", SCRIPT);
            return data;
        }
    }
}
