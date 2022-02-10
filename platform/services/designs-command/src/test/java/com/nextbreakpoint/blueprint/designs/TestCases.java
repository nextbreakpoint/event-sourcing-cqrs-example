package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaConsumerConfig;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.hamcrest.Matchers.notNullValue;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private KafkaTestPolling eventsPolling;

    private String consumerGroupId;

    public TestCases(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        KafkaConsumer<String, String> eventsConsumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig(consumerGroupId));

        eventsPolling = new KafkaTestPolling(eventsConsumer, TestConstants.EVENTS_TOPIC_NAME);

        eventsPolling.startPolling();
    }

    public void after() {
        try {
            vertx.rxClose()
                    .doOnError(Throwable::printStackTrace)
                    .subscribeOn(Schedulers.io())
                    .toCompletable()
                    .await();
        } catch (Exception ignore) {
        }

        scenario.after();
    }

    @NotNull
    public String getVersion() {
        return scenario.getVersion();
    }

    @NotNull
    public URL makeBaseURL(String path) throws MalformedURLException {
        final String normPath = path.startsWith("/") ? path.substring(1) : path;
        return new URL("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/" + normPath);
    }

    @NotNull
    public String makeAuthorization(String user, String role) {
        return scenario.makeAuthorization(user, role);
    }

    @NotNull
    public String getOriginUrl() {
        return "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort();
    }

    @NotNull
    public HttpsTestTarget getHttpsTestTarget() {
        return new HttpsTestTarget(scenario.getServiceHost(), scenario.getServicePort(), "/", true);
    }

    @NotNull
    public KafkaConsumerConfig createConsumerConfig(String groupId) {
        return KafkaConsumerConfig.builder()
                .withBootstrapServers(scenario.getKafkaHost() + ":" + scenario.getKafkaPort())
                .withKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer")
                .withValueDeserializer("org.apache.kafka.common.serialization.StringDeserializer")
                .withAutoOffsetReset("earliest")
                .withEnableAutoCommit("false")
                .withGroupId(groupId)
                .build();
    }

    public String shouldPublishDesignInsertRequestedEventWhenReceivingAInsertDesignRequest() throws MalformedURLException {
        final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

        long timestamp = System.currentTimeMillis();

        eventsPolling.clearMessages();

        final String uuid = submitInsertDesignRequest(authorization, TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT));

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(uuid, TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_INSERT_REQUESTED);
                    assertThat(messages).hasSize(1);
                    InputMessage decodedMessage = messages.get(0);
                    assertThat(decodedMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_INSERT_REQUESTED);
                    assertThat(decodedMessage.getValue().getUuid()).isNotNull();
                    assertThat(decodedMessage.getKey()).isNotNull();
                    assertThat(decodedMessage.getValue().getSource()).isNotNull();
                    assertThat(decodedMessage.getTimestamp()).isGreaterThanOrEqualTo(timestamp);
                    DesignInsertRequested decodedEvent = Json.decodeValue(decodedMessage.getValue().getData(), DesignInsertRequested.class);
                    assertThat(decodedEvent.getDesignId()).isNotNull();
                    assertThat(decodedMessage.getKey()).isEqualTo(decodedEvent.getDesignId().toString());
                    Design decodedDesign = Json.decodeValue(decodedEvent.getData(), Design.class);
                    assertThat(decodedDesign.getManifest()).isEqualTo(TestConstants.MANIFEST);
                    assertThat(decodedDesign.getMetadata()).isEqualTo(TestConstants.METADATA);
                    assertThat(decodedDesign.getScript()).isEqualTo(TestConstants.SCRIPT);
                });

        return uuid;
    }

    public String shouldPublishDesignUpdateRequestedEventWhenReceivingAUpdateDesignRequest() throws MalformedURLException {
        final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

        long timestamp = System.currentTimeMillis();

        eventsPolling.clearMessages();

        final String uuid = UUID.randomUUID().toString();

        submitUpdateDesignRequest(authorization, TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT), uuid);

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(uuid, TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_UPDATE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    InputMessage decodedMessage = messages.get(0);
                    assertThat(decodedMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_UPDATE_REQUESTED);
                    assertThat(decodedMessage.getValue().getUuid()).isNotNull();
                    assertThat(decodedMessage.getKey()).isNotNull();
                    assertThat(decodedMessage.getValue().getSource()).isNotNull();
                    assertThat(decodedMessage.getTimestamp()).isGreaterThanOrEqualTo(timestamp);
                    DesignUpdateRequested decodedEvent = Json.decodeValue(decodedMessage.getValue().getData(), DesignUpdateRequested.class);
                    assertThat(decodedEvent.getDesignId()).isNotNull();
                    assertThat(decodedMessage.getKey()).isEqualTo(decodedEvent.getDesignId().toString());
                    Design decodedDesign = Json.decodeValue(decodedEvent.getData(), Design.class);
                    assertThat(decodedDesign.getManifest()).isEqualTo(TestConstants.MANIFEST);
                    assertThat(decodedDesign.getMetadata()).isEqualTo(TestConstants.METADATA);
                    assertThat(decodedDesign.getScript()).isEqualTo(TestConstants.SCRIPT);
                });

        return uuid;
    }

    public String shouldPublishDesignDeleteRequestedEventWhenReceivingADeleteDesignRequest() throws MalformedURLException {
        final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

        long timestamp = System.currentTimeMillis();

        eventsPolling.clearMessages();

        final String uuid = UUID.randomUUID().toString();

        submitDeleteDesignRequest(authorization, uuid);

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(uuid, TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DELETE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    InputMessage decodedMessage = messages.get(0);
                    assertThat(decodedMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DELETE_REQUESTED);
                    assertThat(decodedMessage.getValue().getUuid()).isNotNull();
                    assertThat(decodedMessage.getKey()).isNotNull();
                    assertThat(decodedMessage.getValue().getSource()).isNotNull();
                    assertThat(decodedMessage.getTimestamp()).isGreaterThanOrEqualTo(timestamp);
                    DesignDeleteRequested decodedEvent = Json.decodeValue(decodedMessage.getValue().getData(), DesignDeleteRequested.class);
                    assertThat(decodedEvent.getDesignId()).isNotNull();
                    assertThat(decodedMessage.getKey()).isEqualTo(decodedEvent.getDesignId().toString());
                });

        return uuid;
    }

    private String submitInsertDesignRequest(String authorization, Map<String, Object> design) throws MalformedURLException {
        return given().config(TestUtils.getRestAssuredConfig())
                .and().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(design)
                .when().post(makeBaseURL("/v1/designs"))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON)
                .and().body("uuid", notNullValue())
                .and().extract().response().body().jsonPath().getString("uuid");
    }

    private void submitUpdateDesignRequest(String authorization, Map<String, Object> design, String uuid) throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(design)
                .when().put(makeBaseURL("/v1/designs/" + uuid))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
    }

    private void submitDeleteDesignRequest(String authorization, String uuid) throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().header(AUTHORIZATION, authorization)
                .and().accept(ContentType.JSON)
                .when().delete(makeBaseURL("/v1/designs/" + uuid))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
    }
}