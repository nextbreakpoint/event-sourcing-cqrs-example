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
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import io.vertx.core.json.Json;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;

public class PactTests {
//    private static final String UUID_REGEXP = "[0-9a-f]{8}-[0-9a-f]{4}-[5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

//    private static final UUID DESIGN_UUID_1 = new UUID(0L, 1L);
//    private static final UUID DESIGN_UUID_2 = new UUID(0L, 2L);
//    private static final UUID DESIGN_UUID_3 = new UUID(0L, 3L);
//    private static final UUID DESIGN_UUID_4 = new UUID(0L, 4L);

    private static final TestScenario scenario = new TestScenario();

    private static final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private static Environment environment = Environment.getDefaultEnvironment();

    private static KafkaTestPolling eventsPolling;
    private static KafkaTestPolling renderPolling;
    private static KafkaTestEmitter eventEmitter;

    private static TestCassandra testCassandra;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.provider.version", scenario.getVersion());

        KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, scenario.createProducerConfig());

        KafkaConsumer<String, String> eventsConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("pact-test-events"));

        KafkaConsumer<String, String> renderConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("pact-test-render"));

        CassandraClient session = CassandraClientFactory.create(environment, vertx, scenario.createCassandraConfig());

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
        public MessagePact designInsertRequested(MessagePactBuilder builder) {
            UUID uuid = UUID.randomUUID();

            PactDslJsonBody payload = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringValue("data", TestConstants.JSON_1)
                    .numberValue("levels", TestConstants.LEVELS);

            PactDslJsonBody value = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload.toString())
                    .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value.toString());

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message)
                    .toPact();
        }

        @Pact(consumer = "designs-event-consumer")
        public MessagePact designUpdateRequested(MessagePactBuilder builder) {
            UUID uuid = UUID.randomUUID();

            PactDslJsonBody payload1 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringValue("data", TestConstants.JSON_1)
                    .numberValue("levels", TestConstants.LEVELS);

            PactDslJsonBody value1 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload1.toString())
                    .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message1 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value1.toString());

            PactDslJsonBody payload2 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringValue("data", TestConstants.JSON_2)
                    .numberValue("levels", TestConstants.LEVELS);

            PactDslJsonBody value2 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload2.toString())
                    .stringValue("type", TestConstants.DESIGN_UPDATE_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message2 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value2.toString());

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message1)
                    .expectsToReceive("command to update design")
                    .withContent(message2)
                    .toPact();
        }

        @Pact(consumer = "designs-event-consumer")
        public MessagePact designDeleteRequested(MessagePactBuilder builder) {
            UUID uuid = UUID.randomUUID();

            PactDslJsonBody payload1 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringValue("data", TestConstants.JSON_1)
                    .numberValue("levels", TestConstants.LEVELS);

            PactDslJsonBody value1 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload1.toString())
                    .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message1 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value1.toString());

            PactDslJsonBody payload2 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased());

            PactDslJsonBody value2 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload2.toString())
                    .stringValue("type", TestConstants.DESIGN_DELETE_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message2 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value2.toString());

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message1)
                    .expectsToReceive("command to delete design")
                    .withContent(message2)
                    .toPact();
        }

        @Pact(consumer = "designs-event-consumer")
        public MessagePact tileRenderCompleted(MessagePactBuilder builder) {
            UUID uuid = UUID.randomUUID();

            PactDslJsonBody payload1 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringValue("data", TestConstants.JSON_1)
                    .numberValue("levels", TestConstants.LEVELS);

            PactDslJsonBody value1 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload1.toString())
                    .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message1 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value1.toString());

            PactDslJsonBody payload2 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringMatcher("esid", "\\d{10}")
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 0)
                    .numberValue("row", 0)
                    .numberValue("col", 0)
                    .stringValue("status", "FAILED");

            PactDslJsonBody value2 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload2.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message2 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value2.toString());

            PactDslJsonBody payload3 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringMatcher("esid", "\\d{10}")
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 1)
                    .numberValue("row", 0)
                    .numberValue("col", 0)
                    .stringValue("status", "COMPLETED");

            PactDslJsonBody value3 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload3.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message3 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value3.toString());

            PactDslJsonBody payload4 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringMatcher("esid", "\\d{10}")
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 1)
                    .numberValue("row", 1)
                    .numberValue("col", 0)
                    .stringValue("status", "COMPLETED");

            PactDslJsonBody value4 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload4.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message4 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value4.toString());

            PactDslJsonBody payload5 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringMatcher("esid", "\\d{10}")
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 1)
                    .numberValue("row", 2)
                    .numberValue("col", 1)
                    .stringValue("status", "COMPLETED");

            PactDslJsonBody value5 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload5.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message5 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value5.toString());

            PactDslJsonBody payload6 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringMatcher("esid", "\\d{10}")
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 1)
                    .numberValue("row", 3)
                    .numberValue("col", 1)
                    .stringValue("status", "COMPLETED");

            PactDslJsonBody value6 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload6.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message6 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value6.toString());

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message1)
                    .expectsToReceive("tile render completed")
                    .withContent(message2)
                    .expectsToReceive("tile render completed")
                    .withContent(message3)
                    .expectsToReceive("tile render completed")
                    .withContent(message4)
                    .expectsToReceive("tile render completed")
                    .withContent(message5)
                    .expectsToReceive("tile render completed")
                    .withContent(message6)
                    .toPact();
        }

        @Test
        @PactTestFor(providerName = "designs-event-producer", port = "1111", pactMethod = "designInsertRequested", providerType = ProviderType.ASYNCH)
        @DisplayName("Should update the design after receiving a DesignInsertRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage(MessagePact messagePact) {
            final KafkaRecord kafkaRecord = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), KafkaRecord.class);

            final OutputMessage designInsertRequestedMessage = OutputMessage.from(kafkaRecord.getKey(), Json.decodeValue(kafkaRecord.getValue(), Payload.class));

            final DesignInsertRequested designInsertRequested = Json.decodeValue(designInsertRequestedMessage.getValue().getData(), DesignInsertRequested.class);

            final UUID designId = designInsertRequested.getUuid();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            eventEmitter.sendMessage(designInsertRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_INSERT_REQUESTED);
                        assertThat(messages).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchMessages(designId);
                        assertThat(rows).hasSize(1);
                        TestAssertions.assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        final List<Tiles> tiles = TestUtils.convertToTilesList(TestUtils.createTilesMap(TestConstants.LEVELS));
                        TestAssertions.assertExpectedDesign(rows.get(0), TestConstants.JSON_1, "CREATED", tiles);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(1);
                        TestAssertions.assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                        messages.forEach(message -> TestAssertions.assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(designId, event, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                    });

            await().atMost(Duration.ofSeconds(20))
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(TestConstants.CHECKSUM_1));
                        assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                        List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(designId, event, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                    });
        }

        @Test
        @PactTestFor(providerName = "designs-event-producer", port = "1112", pactMethod = "designUpdateRequested", providerType = ProviderType.ASYNCH)
        @DisplayName("Should update the design after receiving a DesignUpdateRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage(MessagePact messagePact) {
            final KafkaRecord kafkaRecord1 = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord2 = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), KafkaRecord.class);

            final OutputMessage designInsertRequestedMessage = OutputMessage.from(kafkaRecord1.getKey(), Json.decodeValue(kafkaRecord1.getValue(), Payload.class));

            final OutputMessage designUpdateRequestedMessage = OutputMessage.from(kafkaRecord2.getKey(), Json.decodeValue(kafkaRecord2.getValue(), Payload.class));

            final DesignInsertRequested designInsertRequested = Json.decodeValue(designInsertRequestedMessage.getValue().getData(), DesignInsertRequested.class);

            final UUID designId = designInsertRequested.getUuid();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            eventEmitter.sendMessage(designInsertRequestedMessage);

            eventEmitter.sendMessage(designUpdateRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_INSERT_REQUESTED);
                        assertThat(messages1).hasSize(1);
                        final List<InputMessage> messages2 = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_UPDATE_REQUESTED);
                        assertThat(messages2).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchMessages(designId);
                        assertThat(rows).hasSize(2);
                        TestAssertions.assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                        TestAssertions.assertExpectedMessage(rows.get(1), designUpdateRequestedMessage);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        final List<Tiles> tiles = TestUtils.convertToTilesList(TestUtils.createTilesMap(TestConstants.LEVELS));
                        TestAssertions.assertExpectedDesign(rows.get(0), TestConstants.JSON_2, "UPDATED", tiles);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(2);
                        TestAssertions.assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                        TestAssertions.assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(2);
                        TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED");
                        TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(1), TestConstants.JSON_2, TestConstants.CHECKSUM_2, "UPDATED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS) * 2);
                        messages.forEach(message -> TestAssertions.assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events1 = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                        List<TileRenderRequested> events2 = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_2);
                        assertThat(events1).hasSize(messages.size() / 2);
                        assertThat(events2).hasSize(messages.size() / 2);
                        events1.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(designId, event, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                        events2.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(designId, event, TestConstants.JSON_2, TestConstants.CHECKSUM_2));
                    });

            await().atMost(Duration.ofSeconds(20))
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(TestConstants.CHECKSUM_2));
                        assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                        List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_2);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(designId, event, TestConstants.JSON_2, TestConstants.CHECKSUM_2));
                    });
        }

        @Test
        @PactTestFor(providerName = "designs-event-producer", port = "1113", pactMethod = "designDeleteRequested", providerType = ProviderType.ASYNCH)
        @DisplayName("Should update the design after receiving a DesignDeleteRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage(MessagePact messagePact) {
            final KafkaRecord kafkaRecord1 = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord2 = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), KafkaRecord.class);

            final OutputMessage designInsertRequestedMessage = OutputMessage.from(kafkaRecord1.getKey(), Json.decodeValue(kafkaRecord1.getValue(), Payload.class));

            final OutputMessage designDeleteRequestedMessage = OutputMessage.from(kafkaRecord2.getKey(), Json.decodeValue(kafkaRecord2.getValue(), Payload.class));

            final DesignInsertRequested designInsertRequested = Json.decodeValue(designInsertRequestedMessage.getValue().getData(), DesignInsertRequested.class);

            final UUID designId = designInsertRequested.getUuid();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            eventEmitter.sendMessage(designInsertRequestedMessage);

            eventEmitter.sendMessage(designDeleteRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_INSERT_REQUESTED);
                        assertThat(messages1).hasSize(1);
                        final List<InputMessage> messages2 = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DELETE_REQUESTED);
                        assertThat(messages2).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchMessages(designId);
                        assertThat(rows).hasSize(2);
                        TestAssertions.assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                        TestAssertions.assertExpectedMessage(rows.get(1), designDeleteRequestedMessage);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        final List<Tiles> tiles = TestUtils.convertToTilesList(TestUtils.createTilesMap(TestConstants.LEVELS));
                        TestAssertions.assertExpectedDesign(rows.get(0), TestConstants.JSON_1, "DELETED", tiles);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(2);
                        TestAssertions.assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                        TestAssertions.assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(2);
                        TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED");
                        TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(1), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "DELETED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                        messages.forEach(message -> TestAssertions.assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(designId, event, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                    });

            await().atMost(Duration.ofSeconds(20))
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(TestConstants.CHECKSUM_1));
                        assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                        List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(designId, event, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                    });
        }


        @Test
        @PactTestFor(providerName = "designs-tile-renderer", port = "1114", pactMethod = "tileRenderCompleted", providerType = ProviderType.ASYNCH)
        @DisplayName("Should update the design after receiving a TileRenderCompleted event")
        public void shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage(MessagePact messagePact) {
            final KafkaRecord kafkaRecord1 = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord2 = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord3 = Json.decodeValue(messagePact.getMessages().get(2).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord4 = Json.decodeValue(messagePact.getMessages().get(3).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord5 = Json.decodeValue(messagePact.getMessages().get(4).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord6 = Json.decodeValue(messagePact.getMessages().get(5).contentsAsString(), KafkaRecord.class);

            final OutputMessage designInsertRequestedMessage = OutputMessage.from(kafkaRecord1.getKey(), Json.decodeValue(kafkaRecord1.getValue(), Payload.class));

            final OutputMessage tileRenderCompletedMessage1 = OutputMessage.from(kafkaRecord2.getKey(), Json.decodeValue(kafkaRecord2.getValue(), Payload.class));

            final OutputMessage tileRenderCompletedMessage2 = OutputMessage.from(kafkaRecord3.getKey(), Json.decodeValue(kafkaRecord3.getValue(), Payload.class));

            final OutputMessage tileRenderCompletedMessage3 = OutputMessage.from(kafkaRecord4.getKey(), Json.decodeValue(kafkaRecord4.getValue(), Payload.class));

            final OutputMessage tileRenderCompletedMessage4 = OutputMessage.from(kafkaRecord5.getKey(), Json.decodeValue(kafkaRecord5.getValue(), Payload.class));

            final OutputMessage tileRenderCompletedMessage5 = OutputMessage.from(kafkaRecord6.getKey(), Json.decodeValue(kafkaRecord6.getValue(), Payload.class));

            final DesignInsertRequested designInsertRequested = Json.decodeValue(designInsertRequestedMessage.getValue().getData(), DesignInsertRequested.class);

            final UUID designId = designInsertRequested.getUuid();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            eventEmitter.sendMessage(designInsertRequestedMessage);

            final long[] offset = new long[1];

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_INSERT_REQUESTED);
                        assertThat(messages).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchMessages(designId);
                        assertThat(rows).hasSize(1);
                        TestAssertions.assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        final List<Tiles> tiles = TestUtils.convertToTilesList(TestUtils.createTilesMap(TestConstants.LEVELS));
                        TestAssertions.assertExpectedDesign(rows.get(0), TestConstants.JSON_1, "CREATED", tiles);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(1);
                        TestAssertions.assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                        messages.forEach(message -> TestAssertions.assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(designId, event, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                    });

            await().atMost(Duration.ofSeconds(20))
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(TestConstants.CHECKSUM_1));
                        assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                        List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(designId, event, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                    });

            System.out.println("offset = " + offset[0]);

            eventEmitter.sendMessage(tileRenderCompletedMessage1);
            eventEmitter.sendMessage(tileRenderCompletedMessage2);
            eventEmitter.sendMessage(tileRenderCompletedMessage3);
            eventEmitter.sendMessage(tileRenderCompletedMessage4);
            eventEmitter.sendMessage(tileRenderCompletedMessage5);

            await().atMost(ONE_MINUTE)
                    .pollInterval(TEN_SECONDS)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_AGGREGATE_UPDATE_REQUIRED);
                        assertThat(messages1).hasSize(5);
                        messages1.forEach(message -> TestAssertions.assertExpectedTileAggregateUpdateRequiredMessage(designId, message));
                        final List<InputMessage> messages2 = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages2).hasSize(1);
                        messages2.forEach(message -> TestAssertions.assertExpectedTileAggregateUpdateRequestedMessage(designId, message));
                        final List<InputMessage> messages3 = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages3).hasSize(1);
                        messages3.forEach(message -> TestAssertions.assertExpectedTileAggregateUpdateCompletedMessage(designId, message));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        final Map<Integer, Tiles> tilesMap = TestUtils.createTilesMap(TestConstants.LEVELS);
                        tilesMap.put(0, new Tiles(0, 1, Set.of(), Set.of(0)));
                        tilesMap.put(1, new Tiles(1, 4, Set.of(0, 65536, 131073, 196609), Set.of()));
                        final List<Tiles> tiles = TestUtils.convertToTilesList(tilesMap);
                        TestAssertions.assertExpectedDesign(rows.get(0), TestConstants.JSON_1, "CREATED", tiles);
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
        public String produceDesignAggregateUpdateCompleted1() {
            final UUID designId = UUID.randomUUID();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            eventsPolling.clearMessages();

            eventEmitter.sendMessage(designInsertRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED");
                    });

            final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_AGGREGATE_UPDATE_COMPLETED);
            assertThat(messages).hasSize(1);

            return Json.encode(new KafkaRecord(messages.get(0).getKey(), Json.encode(messages.get(0).getValue())));
        }

        @PactVerifyProvider("design changed event 2")
        public String produceDesignAggregateUpdateCompleted2() {
            final UUID designId = UUID.randomUUID();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            eventsPolling.clearMessages();

            eventEmitter.sendMessage(designInsertRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED");
                    });

            final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, MessageType.DESIGN_AGGREGATE_UPDATE_COMPLETED);
            assertThat(messages).hasSize(1);

            return Json.encode(new KafkaRecord(messages.get(0).getKey(), Json.encode(messages.get(0).getValue())));
        }
    }
}
