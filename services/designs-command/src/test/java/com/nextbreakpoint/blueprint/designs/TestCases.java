package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.drivers.CassandraClientConfig;
import com.nextbreakpoint.blueprint.common.drivers.CassandraClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaConsumerConfig;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.test.TestContext;
import io.restassured.http.ContentType;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final TestContext context = new TestContext();

    private final TestSteps steps = new TestSteps(context, new TestActionsImpl());

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private KafkaTestPolling commandsPolling;
    private KafkaTestPolling eventsPolling;

    private TestCassandra testCassandra;

    private String consumerGroupId;

    public TestCases(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId + "-" + scenario.getUniqueTestId();
    }

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));

        testCassandra = new TestCassandra(CassandraClientFactory.create(createCassandraConfig()));

        KafkaConsumer<String, String> commandsConsumer = KafkaClientFactory.createConsumer(createConsumerConfig(consumerGroupId));

        KafkaConsumer<String, String> eventsConsumer = KafkaClientFactory.createConsumer(createConsumerConfig(consumerGroupId));

        commandsPolling = new KafkaTestPolling(commandsConsumer, TestConstants.COMMANDS_TOPIC_NAME + "-" + scenario.getUniqueTestId());
        eventsPolling = new KafkaTestPolling(eventsConsumer, TestConstants.EVENTS_TOPIC_NAME + "-" + scenario.getUniqueTestId());

        commandsPolling.startPolling();
        eventsPolling.startPolling();

        deleteData();

        context.clear();
    }

    public void after() {
        if (commandsPolling != null) {
            commandsPolling.stopPolling();
        }
        if (eventsPolling != null) {
            eventsPolling.stopPolling();
        }

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
    public TestSteps getSteps() {
        return steps;
    }

    public void deleteData() {
        testCassandra.deleteMessages();
    }

    @NotNull
    public URL makeBaseURL(String path) throws MalformedURLException {
        final String normPath = path.startsWith("/") ? path.substring(1) : path;
        return new URL("http://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/" + normPath);
    }

    @NotNull
    public String makeAuthorization(String user, String role) {
        return scenario.makeAuthorization(user, role);
    }

    @NotNull
    public String getOriginUrl() {
        return "http://" + scenario.getServiceHost() + ":" + scenario.getServicePort();
    }

    @NotNull
    public HttpTestTarget getHttpTestTarget() {
        return new HttpTestTarget(scenario.getServiceHost(), scenario.getServicePort(), "/");
    }

    @NotNull
    private CassandraClientConfig createCassandraConfig() {
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
    private KafkaConsumerConfig createConsumerConfig(String groupId) {
        return KafkaConsumerConfig.builder()
                .withBootstrapServers(scenario.getKafkaHost() + ":" + scenario.getKafkaPort())
                .withKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer")
                .withValueDeserializer("org.apache.kafka.common.serialization.StringDeserializer")
                .withAutoOffsetReset("earliest")
                .withEnableAutoCommit("false")
                .withGroupId(groupId)
                .build();
    }

    private String submitInsertDesignRequest(String authorization, Map<String, String> design) throws MalformedURLException {
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

    private void submitUpdateDesignRequest(String authorization, Map<String, String> design, String uuid) throws MalformedURLException {
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

    private class TestActionsImpl implements TestActions {
        @Override
        public void clearMessages(Source source) {
            polling(source).clearMessages();
        }

        @Override
        public List<InputMessage> findMessages(Source source, String messageSource, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage> messagePredicate) {
            return polling(source).findMessages(messageSource, messageType, keyPredicate, messagePredicate);
        }

        @Override
        public List<Row> fetchMessages(UUID designId, UUID messageId) {
//            return testCassandra.fetchMessages(designId, messageUuid);
            return testCassandra.fetchMessages(designId);
        }

        @Override
        public String makeAuthorization(UUID userId, String authority) {
            return scenario.makeAuthorization(userId.toString(), authority);
        }

        @Override
        public UUID submitInsertDesignRequest(String authorization, Map<String, String> design) throws MalformedURLException {
            return UUID.fromString(TestCases.this.submitInsertDesignRequest(authorization, design));
        }

        @Override
        public void submitUpdateDesignRequest(String authorization, Map<String, String> design, UUID designId) throws MalformedURLException {
            TestCases.this.submitUpdateDesignRequest(authorization, design, designId.toString());
        }

        @Override
        public void submitDeleteDesignRequest(String authorization, UUID designId) throws MalformedURLException {
            TestCases.this.submitDeleteDesignRequest(authorization, designId.toString());
        }

        private KafkaTestPolling polling(Source source) {
            return switch (source) {
                case EVENTS -> eventsPolling;
                case COMMANDS -> commandsPolling;
            };
        }
    }
}