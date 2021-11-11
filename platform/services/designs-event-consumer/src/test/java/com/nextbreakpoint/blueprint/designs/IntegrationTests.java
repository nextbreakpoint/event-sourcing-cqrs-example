package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jayway.restassured.RestAssured;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.junit.jupiter.api.*;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;

public class IntegrationTests {
    private static final Environment environment = Environment.getDefaultEnvironment();

    private static final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private static final TestScenario scenario = new TestScenario();

    private static KafkaTestPolling eventsPolling;
    private static KafkaTestPolling renderPolling;
    private static KafkaTestEmitter eventsEmitter;

    private static TestCassandra testCassandra;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        CassandraClient session = CassandraClientFactory.create(environment, vertx, scenario.createCassandraConfig());

        testCassandra = new TestCassandra(session);

        KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, scenario.createProducerConfig());

        KafkaConsumer<String, String> eventsConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test"));

        KafkaConsumer<String, String> renderConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test-rendering"));

        eventsPolling = new KafkaTestPolling(eventsConsumer, TestConstants.EVENTS_TOPIC_NAME);
        renderPolling = new KafkaTestPolling(renderConsumer, TestConstants.RENDERING_QUEUE_TOPIC_NAME);

        eventsPolling.startPolling();
        renderPolling.startPolling();

        eventsEmitter = new KafkaTestEmitter(producer, TestConstants.EVENTS_TOPIC_NAME);
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
    @Tag("integration")
    @DisplayName("Verify behaviour of designs-event-consumer service")
    public class VerifyServiceApi {
        @AfterEach
        public void reset() {
            RestAssured.reset();
        }

        @Test
        @Order(value = 100)
        @DisplayName("Should update the design after receiving a DesignInsertRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage() {
            final UUID designId = UUID.randomUUID();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            eventsEmitter.sendMessage(designInsertRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), "test", TestConstants.DESIGN_INSERT_REQUESTED);
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

            await().atMost(TEN_SECONDS)
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
        @Order(value = 200)
        @DisplayName("Should update the design after receiving a DesignUpdateRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage() {
            final UUID designId = UUID.randomUUID();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            eventsEmitter.sendMessage(designInsertRequestedMessage);

            final DesignUpdateRequested designUpdateRequested = new DesignUpdateRequested(Uuids.timeBased(), designId, TestConstants.JSON_2, TestConstants.LEVELS);

            final OutputMessage designUpdateRequestedMessage = new DesignUpdateRequestedOutputMapper("test").transform(designUpdateRequested);

            eventsEmitter.sendMessage(designUpdateRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = eventsPolling.findMessages(designId.toString(), "test", TestConstants.DESIGN_INSERT_REQUESTED);
                        assertThat(messages1).hasSize(1);
                        final List<InputMessage> messages2 = eventsPolling.findMessages(designId.toString(), "test", TestConstants.DESIGN_UPDATE_REQUESTED);
                        assertThat(messages2).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchMessages(designId);
                        assertThat(rows).hasSize(2);
                        final Set<UUID> uuids = TestUtils.extractUuids(rows);
                        assertThat(uuids).contains(designId);
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

            await().atMost(TEN_SECONDS)
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
        @Order(value = 300)
        @DisplayName("Should update the design after receiving a DesignDeleteRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage() {
            final UUID designId = UUID.randomUUID();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            eventsEmitter.sendMessage(designInsertRequestedMessage);

            final DesignDeleteRequested designDeleteRequested = new DesignDeleteRequested(Uuids.timeBased(), designId);

            final OutputMessage designDeleteRequestedMessage = new DesignDeleteRequestedOutputMapper("test").transform(designDeleteRequested);

            eventsEmitter.sendMessage(designDeleteRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = eventsPolling.findMessages(designId.toString(), "test", TestConstants.DESIGN_INSERT_REQUESTED);
                        assertThat(messages1).hasSize(1);
                        final List<InputMessage> messages2 = eventsPolling.findMessages(designId.toString(), "test", TestConstants.DESIGN_DELETE_REQUESTED);
                        assertThat(messages2).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchMessages(designId);
                        assertThat(rows).hasSize(2);
                        final Set<UUID> uuids = TestUtils.extractUuids(rows);
                        assertThat(uuids).contains(designId);
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

            await().atMost(TEN_SECONDS)
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
        @Order(value = 400)
        @DisplayName("Should update the design after receiving a TileRenderCompleted event")
        public void shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage() {
            final UUID designId = UUID.randomUUID();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            eventsEmitter.sendMessage(designInsertRequestedMessage);

            final long[] offset = new long[1];

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), "test", TestConstants.DESIGN_INSERT_REQUESTED);
                        assertThat(messages).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = testCassandra.fetchMessages(designId);
                        assertThat(rows).hasSize(1);
                        final Set<UUID> uuids = TestUtils.extractUuids(rows);
                        assertThat(uuids).contains(designId);
                        offset[0] = rows.get(0).getLong("MESSAGE_OFFSET");
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

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(TestConstants.CHECKSUM_1));
                        assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                        List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(designId, event, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                    });

            System.out.println("offset = " + offset[0]);

            final TileRenderCompleted tileRenderCompleted0 = new TileRenderCompleted(Uuids.timeBased(), designId, offset[0], TestConstants.CHECKSUM_1, 0, 0, 0, "FAILED");
            final TileRenderCompleted tileRenderCompleted1 = new TileRenderCompleted(Uuids.timeBased(), designId, offset[0], TestConstants.CHECKSUM_1, 1, 0, 0, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted2 = new TileRenderCompleted(Uuids.timeBased(), designId, offset[0], TestConstants.CHECKSUM_1, 1, 1, 0, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted3 = new TileRenderCompleted(Uuids.timeBased(), designId, offset[0], TestConstants.CHECKSUM_1, 1, 2, 1, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted4 = new TileRenderCompleted(Uuids.timeBased(), designId, offset[0], TestConstants.CHECKSUM_1, 1, 3, 1, "COMPLETED");

            final OutputMessage tileRenderCompletedMessage0 = new TileRenderCompletedOutputMapper("test").transform(tileRenderCompleted0);
            final OutputMessage tileRenderCompletedMessage1 = new TileRenderCompletedOutputMapper("test").transform(tileRenderCompleted1);
            final OutputMessage tileRenderCompletedMessage2 = new TileRenderCompletedOutputMapper("test").transform(tileRenderCompleted2);
            final OutputMessage tileRenderCompletedMessage3 = new TileRenderCompletedOutputMapper("test").transform(tileRenderCompleted3);
            final OutputMessage tileRenderCompletedMessage4 = new TileRenderCompletedOutputMapper("test").transform(tileRenderCompleted4);

            eventsEmitter.sendMessage(tileRenderCompletedMessage0);
            eventsEmitter.sendMessage(tileRenderCompletedMessage1);
            eventsEmitter.sendMessage(tileRenderCompletedMessage2);
            eventsEmitter.sendMessage(tileRenderCompletedMessage3);
            eventsEmitter.sendMessage(tileRenderCompletedMessage4);

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
}
