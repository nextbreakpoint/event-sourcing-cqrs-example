package com.nextbreakpoint.shop.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.shop.common.model.Authority;
import com.nextbreakpoint.shop.common.model.Design;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignEvent;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignsEvent;
import com.nextbreakpoint.shop.common.model.events.InsertDesignEvent;
import com.nextbreakpoint.shop.common.model.events.UpdateDesignEvent;
import com.nextbreakpoint.shop.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.shop.common.vertx.TestHelper;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.TWO_SECONDS;

@Tag("slow")
@DisplayName("Designs Command Service")
public class VerticleIT {
    private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
    private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
    private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";

    private static RestAssuredConfig restAssuredConfig;

    private Vertx vertx;

    @BeforeAll
    public static void configureRestAssured() {
        final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
        final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
        final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        restAssuredConfig = RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
    }

    @AfterAll
    public static void unconfigureRestAssured() {
        RestAssured.reset();
    }

    @BeforeEach
    public void createVertx() {
        vertx = new Vertx(io.vertx.core.Vertx.vertx());
    }

    @AfterEach
    public void destroyVertx() {
        vertx.close();
    }

    @Test
    @DisplayName("Should allow OPTIONS on /designs without access token")
    public void shouldAllowOptionsOnDesignsWithoutAccessToken() throws MalformedURLException {
        given().config(restAssuredConfig)
                .with().header("Origin", "https://localhost:8080")
                .when().options(makeBaseURL("/api/designs"))
                .then().assertThat().statusCode(204)
                .and().header("Access-Control-Allow-Origin", "https://localhost:8080")
                .and().header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Should allow OPTIONS on /designs/id without access token")
    public void shouldAllowOptionsOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
        given().config(restAssuredConfig)
                .with().header("Origin", "https://localhost:8080")
                .when().options(makeBaseURL("/api/designs/" + UUID.randomUUID().toString()))
                .then().assertThat().statusCode(204)
                .and().header("Access-Control-Allow-Origin", "https://localhost:8080")
                .and().header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Should forbid POST on /designs without access token")
    public void shouldForbidPostOnDesignsWithoutAccessToken() throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(createPostData())
                .when().post(makeBaseURL("/api/designs"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid PUT on /designs/id without access token")
    public void shouldForbidPutOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(createPostData())
                .when().put(makeBaseURL("/api/designs/" + UUID.randomUUID().toString()))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid DELETE on /designs/id without access token")
    public void shouldForbidDeleteOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
        final String uuid = UUID.randomUUID().toString();

        given().config(restAssuredConfig)
                .and().accept(ContentType.JSON)
                .when().delete(makeBaseURL("/api/designs/" + uuid))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid DELETE on /designs without access token")
    public void shouldForbidDeleteOnDesignsWithoutAccessToken() throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().accept(ContentType.JSON)
                .when().delete(makeBaseURL("/api/designs"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should send a message after accepting POST on /designs")
    public void shouldAcceptPostOnDesigns() throws IOException {
        final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

        pause();

        KafkaConsumer<String, String> consumer = null;

        try {
            consumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig("insert-design"));

            String[] message = new String[]{null};

            consumer.handler(record -> message[0] = record.value())
                    .rxSubscribe("designs-events")
                    .subscribe();

            long timestamp = System.currentTimeMillis();

            createDesign(authorization, createPostData());

            await().atMost(TWO_SECONDS)
                    .pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(message[0]).isNotNull();
                        Message decodedMessage = Json.decodeValue(message[0], Message.class);
                        assertThat(decodedMessage.getMessageType()).isEqualTo("design-insert");
                        assertThat(decodedMessage.getMessageId()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isNotNull();
                        assertThat(decodedMessage.getMessageSource()).isNotNull();
                        assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                        InsertDesignEvent decodedEvent = Json.decodeValue(decodedMessage.getMessageBody(), InsertDesignEvent.class);
                        assertThat(decodedEvent.getUuid()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isEqualTo(decodedEvent.getUuid().toString());
                        Design decodedDesign = Json.decodeValue(decodedEvent.getJson(), Design.class);
                        assertThat(decodedDesign.getManifest()).isEqualTo(MANIFEST);
                        assertThat(decodedDesign.getMetadata()).isEqualTo(METADATA);
                        assertThat(decodedDesign.getScript()).isEqualTo(SCRIPT);
                    });
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    @Test
    @DisplayName("Should send a message after accepting PUT on /designs/id")
    public void shouldAcceptPutOnDesignsSlashId() throws IOException {
        final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

        pause();

        KafkaConsumer<String, String> consumer = null;

        try {
            consumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig("update-design"));

            String[] message = new String[]{null};

            consumer.handler(record -> message[0] = record.value())
                    .rxSubscribe("designs-events")
                    .subscribe();

            final String uuid = UUID.randomUUID().toString();

            long timestamp = System.currentTimeMillis();

            updateDesign(authorization, createPostData(), uuid);

            await().atMost(TWO_SECONDS)
                    .pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(message[0]).isNotNull();
                        Message decodedMessage = Json.decodeValue(message[0], Message.class);
                        assertThat(decodedMessage.getMessageType()).isEqualTo("design-update");
                        assertThat(decodedMessage.getMessageId()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isEqualTo(uuid);
                        assertThat(decodedMessage.getMessageSource()).isNotNull();
                        assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                        UpdateDesignEvent decodedEvent = Json.decodeValue(decodedMessage.getMessageBody(), UpdateDesignEvent.class);
                        assertThat(decodedEvent.getUuid()).isNotNull();
                        assertThat(decodedEvent.getUuid().toString()).isEqualTo(uuid);
                        Design decodedDesign = Json.decodeValue(decodedEvent.getJson(), Design.class);
                        assertThat(decodedDesign.getManifest()).isEqualTo(MANIFEST);
                        assertThat(decodedDesign.getMetadata()).isEqualTo(METADATA);
                        assertThat(decodedDesign.getScript()).isEqualTo(SCRIPT);
                    });
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    @Test
    @DisplayName("Should send a message after accepting DELETE on /designs/id")
    public void shouldAcceptDeleteOnDesignsSlashId() throws IOException {
        final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

        pause();

        KafkaConsumer<String, String> consumer = null;

        try {
            consumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig("detele-design"));

            String[] message = new String[]{null};

            consumer.handler(record -> message[0] = record.value())
                    .rxSubscribe("designs-events")
                    .subscribe();

            final String uuid = UUID.randomUUID().toString();

            long timestamp = System.currentTimeMillis();

            deleteDesign(authorization, uuid);

            await().atMost(TWO_SECONDS)
                    .pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(message[0]).isNotNull();
                        Message decodedMessage = Json.decodeValue(message[0], Message.class);
                        assertThat(decodedMessage.getMessageType()).isEqualTo("design-delete");
                        assertThat(decodedMessage.getMessageId()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isEqualTo(uuid);
                        assertThat(decodedMessage.getMessageSource()).isNotNull();
                        assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                        DeleteDesignEvent decodedEvent = Json.decodeValue(decodedMessage.getMessageBody(), DeleteDesignEvent.class);
                        assertThat(decodedEvent.getUuid()).isNotNull();
                        assertThat(decodedEvent.getUuid().toString()).isEqualTo(uuid);
                    });
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    @Test
    @DisplayName("Should send a message after accepting DELETE on /designs")
    public void shouldAcceptDeleteOnDesigns() throws IOException {
        final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

        pause();

        KafkaConsumer<String, String> consumer = null;

        try {
            consumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig("delete-designs"));

            String[] message = new String[]{null};

            consumer.handler(record -> message[0] = record.value())
                    .rxSubscribe("designs-events")
                    .subscribe();

            final String uuid = new UUID(0, 0).toString();

            long timestamp = System.currentTimeMillis();

            deleteDesigns(authorization);

            await().atMost(TWO_SECONDS)
                    .pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(message[0]).isNotNull();
                        Message decodedMessage = Json.decodeValue(message[0], Message.class);
                        assertThat(decodedMessage.getMessageType()).isEqualTo("designs-delete");
                        assertThat(decodedMessage.getMessageId()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isEqualTo(uuid);
                        assertThat(decodedMessage.getMessageSource()).isNotNull();
                        assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                        DeleteDesignsEvent decodedEvent = Json.decodeValue(decodedMessage.getMessageBody(), DeleteDesignsEvent.class);
                    });
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    private void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
    }

    private void deleteDesign(String authorization, String uuid) throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().header(AUTHORIZATION, authorization)
                .and().accept(ContentType.JSON)
                .when().delete(makeBaseURL("/api/designs/" + uuid))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
    }

    private void deleteDesigns(String authorization) throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().header(AUTHORIZATION, authorization)
                .and().accept(ContentType.JSON)
                .when().delete(makeBaseURL("/api/designs"))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
    }

    private void createDesign(String authorization, Map<String, String> design) throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(design)
                .when().post(makeBaseURL("/api/designs"))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
//            .and().body("messageId", notNullValue())
//            .and().extract().response().body().jsonPath().getString("messageId");
    }

    private void updateDesign(String authorization, Map<String, String> design, String uuid) throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(design)
                .when().put(makeBaseURL("/api/designs/" + uuid))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
    }

    private JsonObject createConsumerConfig(String group) {
        final JsonObject config = new JsonObject();
        config.put("kafka_bootstrapServers", System.getProperty("stub.host", "localhost") + ":9092");
        config.put("kafka_group_id", group);
        return config;
    }

    private Map<String, String> createPostData() {
        final Map<String, String> data = new HashMap<>();
        data.put("manifest", MANIFEST);
        data.put("metadata", METADATA);
        data.put("script", SCRIPT);
        return data;
    }

    private URL makeBaseURL(String path) throws MalformedURLException {
        final Integer port = Integer.getInteger("http.port", 3001);
        final String normPath = path.startsWith("/") ? path.substring(1) : path;
        return new URL("https://localhost:" + port + "/" + normPath);
    }
}
