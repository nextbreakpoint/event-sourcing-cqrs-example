package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import com.datastax.oss.driver.api.core.cql.Row;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientConfig;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaConsumerConfig;
import io.vertx.rxjava.cassandra.CassandraClient;
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

    private KafkaTestPolling commandsPolling;
    private KafkaTestPolling eventsPolling;

    private TestCassandra testCassandra;

    private String consumerGroupId;

    public TestCases(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        CassandraClient session = CassandraClientFactory.create(vertx, createCassandraConfig());

        testCassandra = new TestCassandra(session);

        KafkaConsumer<String, String> commandsConsumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig(consumerGroupId));

        KafkaConsumer<String, String> eventsConsumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig(consumerGroupId));

        commandsPolling = new KafkaTestPolling(commandsConsumer, TestConstants.COMMANDS_TOPIC_NAME);
        eventsPolling = new KafkaTestPolling(eventsConsumer, TestConstants.EVENTS_TOPIC_NAME);

        commandsPolling.startPolling();
        eventsPolling.startPolling();

        testCassandra.deleteMessages();
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
    public CassandraClientConfig createCassandraConfig() {
        return CassandraClientConfig.builder()
                .withClusterName("datacenter1")
                .withKeyspace(TestConstants.DATABASE_KEYSPACE)
                .withUsername("admin")
                .withPassword("password")
                .withContactPoints(new String[] { scenario.getCassandraHost() })
                .withPort(scenario.getCassandraPort())
                .build();
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
        final String authorization = scenario.makeAuthorization(TestConstants.USER_ID.toString(), Authority.ADMIN);

        long timestamp = System.currentTimeMillis();

        commandsPolling.clearMessages();
        eventsPolling.clearMessages();

        final String uuid = submitInsertDesignRequest(authorization, TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT));

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = commandsPolling.findMessages(uuid, TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_INSERT_COMMAND);
                    assertThat(messages).hasSize(1);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<Row> rows = testCassandra.fetchMessages(UUID.fromString(uuid));
                    assertThat(rows).hasSize(1);
                    TestAssertions.assertExpectedDesignInsertCommand(rows.get(0), uuid);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(uuid, TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_INSERT_REQUESTED);
                    assertThat(messages).hasSize(1);
                    InputMessage decodedMessage = messages.get(0);
                    TestAssertions.assertExpectedDesignInsertRequestedMessage(decodedMessage, uuid);
                });

        return uuid;
    }

    public String shouldPublishDesignUpdateRequestedEventWhenReceivingAUpdateDesignRequest() throws MalformedURLException {
        final String authorization = scenario.makeAuthorization(TestConstants.USER_ID.toString(), Authority.ADMIN);

        long timestamp = System.currentTimeMillis();

        commandsPolling.clearMessages();
        eventsPolling.clearMessages();

        final String uuid = UUID.randomUUID().toString();

        submitUpdateDesignRequest(authorization, TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT), uuid);

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = commandsPolling.findMessages(uuid, TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_UPDATE_COMMAND);
                    assertThat(messages).hasSize(1);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<Row> rows = testCassandra.fetchMessages(UUID.fromString(uuid));
                    assertThat(rows).hasSize(1);
                    TestAssertions.assertExpectedDesignUpdateCommand(rows.get(0), uuid);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(uuid, TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_UPDATE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    InputMessage decodedMessage = messages.get(0);
                    TestAssertions.assertExpectedDesignUpdateRequestedMessage(decodedMessage, uuid);
                });

        return uuid;
    }

    public String shouldPublishDesignDeleteRequestedEventWhenReceivingADeleteDesignRequest() throws MalformedURLException {
        final String authorization = scenario.makeAuthorization(TestConstants.USER_ID.toString(), Authority.ADMIN);

        long timestamp = System.currentTimeMillis();

        commandsPolling.clearMessages();
        eventsPolling.clearMessages();

        final String uuid = UUID.randomUUID().toString();

        submitDeleteDesignRequest(authorization, uuid);

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = commandsPolling.findMessages(uuid, TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DELETE_COMMAND);
                    assertThat(messages).hasSize(1);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<Row> rows = testCassandra.fetchMessages(UUID.fromString(uuid));
                    assertThat(rows).hasSize(1);
                    TestAssertions.assertExpectedDesignDeleteCommand(rows.get(0), uuid);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(uuid, TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DELETE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    InputMessage decodedMessage = messages.get(0);
                    TestAssertions.assertExpectedDesignDeleteRequestedMessage(decodedMessage, uuid);
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