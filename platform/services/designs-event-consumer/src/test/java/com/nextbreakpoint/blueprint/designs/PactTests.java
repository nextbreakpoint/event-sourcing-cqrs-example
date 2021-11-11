package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.AmpqTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedEvent;
import io.vertx.core.json.Json;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

@Disabled
public class PactTests {
    private static final String UUID_REGEXP = "[0-9a-f]{8}-[0-9a-f]{4}-[5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    private static final UUID DESIGN_UUID_1 = new UUID(0L, 1L);
    private static final UUID DESIGN_UUID_2 = new UUID(0L, 2L);
    private static final UUID DESIGN_UUID_3 = new UUID(0L, 3L);

    private static final TestScenario scenario = new TestScenario();

    private static final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private static Environment environment = Environment.getDefaultEnvironment();

    private static KafkaTestPolling eventsPolling;
    private static KafkaTestPolling renderPolling;
    private static KafkaTestEmitter eventEmitter;

    private static CassandraClient session;
    private static TestCassandra testCassandra;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.provider.version", scenario.getVersion());

        KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, scenario.createProducerConfig());

        KafkaConsumer<String, String> eventsConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test"));

        KafkaConsumer<String, String> renderConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test-rendering"));

        session = CassandraClientFactory.create(environment, vertx, scenario.createCassandraConfig());

        testCassandra = new TestCassandra(session);

        eventsPolling = new KafkaTestPolling(eventsConsumer, TestConstants.EVENTS_TOPIC_NAME);
        renderPolling = new KafkaTestPolling(renderConsumer, TestConstants.RENDERING_QUEUE_TOPIC_NAME);

        eventsPolling.startPolling();
        renderPolling.startPolling();

        eventEmitter = new KafkaTestEmitter(producer, TestConstants.EVENTS_TOPIC_NAME);
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        try {
            vertx.rxClose()
                    .doOnError(Throwable::printStackTrace)
                    .subscribeOn(Schedulers.io())
                    .toBlocking()
                    .value();
        } catch (Exception ignore) {
        }

        scenario.after();
    }

    @Nested
    @Tag("slow")
    @Tag("pact")
    @DisplayName("Test designs-event-consumer pact")
    @ExtendWith(PactConsumerTestExt.class)
    public class TestDesignsCommandConsumer {
        @Pact(consumer = "designs-event-consumer")
        public MessagePact insertDesign(MessagePactBuilder builder) {
            PactDslJsonBody body = new PactDslJsonBody()
                    .stringValue("uuid", DESIGN_UUID_1.toString())
                    .stringValue("json", TestConstants.JSON_1)
                    .stringMatcher("timestamp", "\\d{10}");

            PactDslJsonBody message = new PactDslJsonBody()
                    .stringMatcher("messageId", DESIGN_UUID_1.toString())
                    .stringValue("messageType", "design-insert")
                    .stringValue("messageBody", body.toString())
                    .stringValue("messageSource", "service-designs")
                    .stringMatcher("partitionKey", UUID_REGEXP)
                    .stringMatcher("timestamp", "\\d{10}");

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message)
                    .toPact();
        }

        @Pact(consumer = "designs-event-consumer")
        public MessagePact updateDesign(MessagePactBuilder builder) {
            PactDslJsonBody body1 = new PactDslJsonBody()
                    .stringMatcher("uuid", DESIGN_UUID_2.toString())
                    .stringValue("json", TestConstants.JSON_1)
                    .stringMatcher("timestamp", "\\d{10}");

            PactDslJsonBody message1 = new PactDslJsonBody()
                    .stringMatcher("messageId", DESIGN_UUID_2.toString())
                    .stringValue("messageType", "design-insert")
                    .stringValue("messageBody", body1.toString())
                    .stringValue("messageSource", "service-designs")
                    .stringMatcher("partitionKey", UUID_REGEXP)
                    .stringMatcher("timestamp", "\\d{10}");

            PactDslJsonBody body2 = new PactDslJsonBody()
                    .stringMatcher("uuid", DESIGN_UUID_2.toString())
                    .stringValue("json", TestConstants.JSON_2)
                    .stringMatcher("timestamp", "\\d{10}");

            PactDslJsonBody message2 = new PactDslJsonBody()
                    .stringMatcher("messageId", DESIGN_UUID_2.toString())
                    .stringValue("messageType", "design-update")
                    .stringValue("messageBody", body2.toString())
                    .stringValue("messageSource", "service-designs")
                    .stringMatcher("partitionKey", UUID_REGEXP)
                    .stringMatcher("timestamp", "\\d{10}");

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message1)
                    .expectsToReceive("command to update design")
                    .withContent(message2)
                    .toPact();
        }

        @Pact(consumer = "designs-event-consumer")
        public MessagePact deleteDesign(MessagePactBuilder builder) {
            PactDslJsonBody body1 = new PactDslJsonBody()
                    .stringMatcher("uuid", DESIGN_UUID_3.toString())
                    .stringValue("json", TestConstants.JSON_1)
                    .stringMatcher("timestamp", "\\d{10}");

            PactDslJsonBody message1 = new PactDslJsonBody()
                    .stringMatcher("messageId", DESIGN_UUID_3.toString())
                    .stringValue("messageType", "design-insert")
                    .stringValue("messageBody", body1.toString())
                    .stringValue("messageSource", "service-designs")
                    .stringMatcher("partitionKey", UUID_REGEXP)
                    .stringMatcher("timestamp", "\\d{10}");

            PactDslJsonBody body2 = new PactDslJsonBody()
                    .stringMatcher("uuid", DESIGN_UUID_3.toString())
                    .stringMatcher("timestamp", "\\d{10}");

            PactDslJsonBody message2 = new PactDslJsonBody()
                    .stringMatcher("messageId", DESIGN_UUID_3.toString())
                    .stringValue("messageType", "design-delete")
                    .stringValue("messageBody", body2.toString())
                    .stringValue("messageSource", "service-designs")
                    .stringMatcher("partitionKey", UUID_REGEXP)
                    .stringMatcher("timestamp", "\\d{10}");

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message1)
                    .expectsToReceive("command to delete design")
                    .withContent(message2)
                    .toPact();
        }

        @Test
        @PactTestFor(providerName = "designs-event-producer", port = "1111", pactMethod = "insertDesign", providerType = ProviderType.ASYNCH)
        public void shouldInsertDesign(MessagePact messagePact) {
            final OutputMessage insertDesignMessage = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), OutputMessage.class);

            final DesignInsertRequested designInsertEvent = Json.decodeValue(insertDesignMessage.getValue().getData(), DesignInsertRequested.class);

            final UUID designId = designInsertEvent.getUuid();

            eventsPolling.clearMessages();

            eventEmitter.sendMessage(insertDesignMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM DESIGN_EVENT WHERE DESIGN_UUID = ?")
                                .map(stmt -> stmt.bind(designId))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(1);
                        rows.forEach(row -> {
                            String actualJson = row.get("DESIGN_DATA", String.class);
                            String actualStatus = row.get("DESIGN_STATUS", String.class);
                            String actualChecksum = row.get("DESIGN_CHECKSUM", String.class);
                            Instant actualPublished = row.getInstant("DESIGN_UPDATED") ;
                            assertThat(actualJson).isEqualTo(TestConstants.JSON_1);
                            assertThat(actualStatus).isEqualTo("CREATED");
                            assertThat(actualChecksum).isNotNull();
                            assertThat(actualPublished).isNotNull();
                        });
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM DESIGN_AGGREGATE WHERE DESIGN_UUID = ?")
                                .map(stmt -> stmt.bind(designId))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(1);
                        rows.forEach(row -> {
                            String actualJson = row.get("DESIGN_DATA", String.class);
                            String actualChecksum = row.get("DESIGN_CHECKSUM", String.class);
                            assertThat(actualJson).isEqualTo(TestConstants.JSON_1);
                            assertThat(actualChecksum).isNotNull();
                        });
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_CHANGED);
                        assertThat(messages).hasSize(1);
                        final InputMessage actualMessage = messages.get(messages.size() - 1);
                        assertThat(actualMessage.getTimestamp()).isNotNull();
                        assertThat(actualMessage.getValue().getSource()).isEqualTo("service-designs");
                        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
                        assertThat(actualMessage.getValue().getUuid()).isNotNull();
                        assertThat(actualMessage.getValue().getType()).isEqualTo("design-changed");
                        DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignChangedEvent.class);
                        assertThat(actualEvent.getUuid()).isEqualTo(designId);
                        assertThat(actualEvent.getTimestamp()).isNotNull();
                    });
        }

        @Test
        @PactTestFor(providerName = "designs-event-producer", port = "1112", pactMethod = "updateDesign", providerType = ProviderType.ASYNCH)
        public void shouldUpdateDesign(MessagePact messagePact) {
            final OutputMessage insertDesignMessage = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), OutputMessage.class);

            final OutputMessage updateDesignMessage = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), OutputMessage.class);

            final DesignInsertRequested designInsertEvent = Json.decodeValue(insertDesignMessage.getValue().getData(), DesignInsertRequested.class);

            final UUID designId = designInsertEvent.getUuid();

            eventsPolling.clearMessages();

            eventEmitter.sendMessage(insertDesignMessage);

            eventEmitter.sendMessage(updateDesignMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM DESIGN_EVENT WHERE DESIGN_UUID = ?")
                                .map(stmt -> stmt.bind(designId))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(2);
                        final Set<UUID> uuids = rows.stream()
                                .map(row -> row.getUuid("DESIGN_UUID"))
                                .collect(Collectors.toSet());
                        assertThat(uuids).contains(designId);
                        String actualJson1 = rows.get(0).get("DESIGN_DATA", String.class);
                        String actualStatus1 = rows.get(0).get("DESIGN_STATUS", String.class);
                        Instant actualPublished1 = rows.get(0).getInstant("DESIGN_UPDATED") ;
                        assertThat(actualJson1).isEqualTo(TestConstants.JSON_1);
                        assertThat(actualStatus1).isEqualTo("CREATED");
                        assertThat(actualPublished1).isNotNull();
                        String actualJson2 = rows.get(1).get("DESIGN_DATA", String.class);
                        String actualStatus2 = rows.get(1).get("DESIGN_STATUS", String.class);
                        Instant actualPublished2 = rows.get(1).getInstant("DESIGN_UPDATED") ;
                        assertThat(actualJson2).isEqualTo(TestConstants.JSON_2);
                        assertThat(actualStatus2).isEqualTo("UPDATED");
                        assertThat(actualPublished2).isNotNull();
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM DESIGN_AGGREGATE WHERE DESIGN_UUID = ?")
                                .map(stmt -> stmt.bind(designId))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(1);
                        rows.forEach(row -> {
                            String actualJson = row.get("DESIGN_DATA", String.class);
                            String actualChecksum = row.get("DESIGN_CHECKSUM", String.class);
                            assertThat(actualJson).isEqualTo(TestConstants.JSON_2);
                            assertThat(actualChecksum).isNotNull();
                        });
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_CHANGED);
                        assertThat(messages).hasSize(2);
                        final InputMessage actualMessage = messages.get(messages.size() - 1);
                        assertThat(actualMessage.getTimestamp()).isNotNull();
                        assertThat(actualMessage.getValue().getSource()).isEqualTo("service-designs");
                        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
                        assertThat(actualMessage.getValue().getUuid()).isNotNull();
                        assertThat(actualMessage.getValue().getType()).isEqualTo("design-changed");
                        DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignChangedEvent.class);
                        assertThat(actualEvent.getUuid()).isEqualTo(designId);
                        assertThat(actualEvent.getTimestamp()).isNotNull();
                    });
        }

        @Test
        @PactTestFor(providerName = "designs-event-producer", port = "1113", pactMethod = "deleteDesign", providerType = ProviderType.ASYNCH)
        public void shouldDeleteDesign(MessagePact messagePact) {
            final OutputMessage insertDesignMessage = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), OutputMessage.class);

            final OutputMessage deleteDesignMessage = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), OutputMessage.class);

            final DesignInsertRequested designInsertEvent = Json.decodeValue(insertDesignMessage.getValue().getData(), DesignInsertRequested.class);

            final UUID designId = designInsertEvent.getUuid();

            eventsPolling.clearMessages();

            eventEmitter.sendMessage(insertDesignMessage);

            eventEmitter.sendMessage(deleteDesignMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM DESIGN_EVENT WHERE DESIGN_UUID = ?")
                                .map(stmt -> stmt.bind(designId))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(2);
                        final Set<UUID> uuids = rows.stream()
                                .map(row -> row.getUuid("DESIGN_UUID"))
                                .collect(Collectors.toSet());
                        assertThat(uuids).contains(designId);
                        String actualJson1 = rows.get(0).get("DESIGN_DATA", String.class);
                        String actualStatus1 = rows.get(0).get("DESIGN_STATUS", String.class);
                        Instant actualPublished1 = rows.get(0).getInstant("DESIGN_UPDATED") ;
                        assertThat(actualJson1).isEqualTo(TestConstants.JSON_1);
                        assertThat(actualStatus1).isEqualTo("CREATED");
                        assertThat(actualPublished1).isNotNull();
                        String actualJson2 = rows.get(1).get("DESIGN_DATA", String.class);
                        String actualStatus2 = rows.get(1).get("DESIGN_STATUS", String.class);
                        Instant actualPublished2 = rows.get(1).getInstant("DESIGN_UPDATED") ;
                        assertThat(actualJson2).isNull();
                        assertThat(actualStatus2).isEqualTo("DELETED");
                        assertThat(actualPublished2).isNotNull();
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM DESIGN_AGGREGATE WHERE DESIGN_UUID = ?")
                                .map(stmt -> stmt.bind(designId))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(0);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_CHANGED);
                        assertThat(messages).hasSize(2);
                        final InputMessage actualMessage = messages.get(messages.size() - 1);
                        assertThat(actualMessage.getTimestamp()).isNotNull();
                        assertThat(actualMessage.getValue().getSource()).isEqualTo("service-designs");
                        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
                        assertThat(actualMessage.getValue().getUuid()).isNotNull();
                        assertThat(actualMessage.getValue().getType()).isEqualTo("design-changed");
                        DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignChangedEvent.class);
                        assertThat(actualEvent.getUuid()).isEqualTo(designId);
                        assertThat(actualEvent.getTimestamp()).isNotNull();
                    });
        }
    }

    @Nested
    @Tag("pact")
    @DisplayName("Verify contract between designs-event-consumer and designs-notification-dispatcher")
    @Provider("designs-event-consumer")
    @Consumer("designs-notification-dispatcher")
    @PactBroker
    public class VerifyDesignsNotificationDispatcher {
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

        @PactVerifyProvider("design changed event 1")
        public String produceDesignChanged1() {
            final UUID designId = DESIGN_UUID_1;

            final DesignInsertRequested designInsertEvent = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, 3);

            final OutputMessage insertDesignMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertEvent);

            eventsPolling.clearMessages();

            eventEmitter.sendMessage(insertDesignMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_CHANGED);
                        assertThat(messages).hasSize(2);
                        final InputMessage actualMessage = messages.get(messages.size() - 1);
                        assertThat(actualMessage.getTimestamp()).isNotNull();
                        assertThat(actualMessage.getValue().getSource()).isEqualTo("service-designs");
                        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
                        assertThat(actualMessage.getValue().getUuid()).isNotNull();
                        assertThat(actualMessage.getValue().getType()).isEqualTo("design-changed");
                        DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignChangedEvent.class);
                        assertThat(actualEvent.getUuid()).isEqualTo(designId);
                        assertThat(actualEvent.getTimestamp()).isNotNull();
                    });

            final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_CHANGED);
            assertThat(messages.isEmpty()).isFalse();
            InputMessage decodedMessage = messages.get(messages.size() - 1);

            return Json.encode(decodedMessage);
        }

        @PactVerifyProvider("design changed event 2")
        public String produceDesignChanged2() {
            final UUID designId = DESIGN_UUID_2;

            final DesignInsertRequested designInsertEvent = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, 3);

            final OutputMessage insertDesignMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertEvent);

            eventsPolling.clearMessages();

            eventEmitter.sendMessage(insertDesignMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_CHANGED);
                        assertThat(messages).hasSize(2);
                        final InputMessage actualMessage = messages.get(messages.size() - 1);
                        assertThat(actualMessage.getTimestamp()).isNotNull();
                        assertThat(actualMessage.getValue().getSource()).isEqualTo("service-designs");
                        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
                        assertThat(actualMessage.getValue().getUuid()).isNotNull();
                        assertThat(actualMessage.getValue().getType()).isEqualTo("design-changed");
                        DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignChangedEvent.class);
                        assertThat(actualEvent.getUuid()).isEqualTo(designId);
                        assertThat(actualEvent.getTimestamp()).isNotNull();
                    });

            final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_CHANGED);
            assertThat(messages.isEmpty()).isFalse();
            InputMessage decodedMessage = messages.get(messages.size() - 1);

            return Json.encode(decodedMessage);
        }
    }
}
