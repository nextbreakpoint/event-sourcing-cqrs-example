package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.drivers.*;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private KafkaTestPolling eventsPolling;
    private KafkaTestPolling renderPolling;
    private KafkaTestEmitter eventEmitter;
    private KafkaTestEmitter renderEmitter;

    private TestCassandra testCassandra;

    private String consumerGroupId;

    public TestCases(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));

        testCassandra = new TestCassandra(CassandraClientFactory.create(createCassandraConfig()));

        KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(createProducerConfig("integration"));

        KafkaConsumer<String, String> eventsConsumer = KafkaClientFactory.createConsumer(createConsumerConfig(consumerGroupId));

        KafkaConsumer<String, String> renderConsumer = KafkaClientFactory.createConsumer(createConsumerConfig(consumerGroupId));

        final Set<String> renderTopics = Set.of(
                TestConstants.RENDER_TOPIC_PREFIX + "-completed-0",
                TestConstants.RENDER_TOPIC_PREFIX + "-completed-1",
                TestConstants.RENDER_TOPIC_PREFIX + "-completed-2",
                TestConstants.RENDER_TOPIC_PREFIX + "-completed-3",
                TestConstants.RENDER_TOPIC_PREFIX + "-requested-0",
                TestConstants.RENDER_TOPIC_PREFIX + "-requested-1",
                TestConstants.RENDER_TOPIC_PREFIX + "-requested-2",
                TestConstants.RENDER_TOPIC_PREFIX + "-requested-3"
        );

        eventsPolling = new KafkaTestPolling(eventsConsumer, TestConstants.EVENTS_TOPIC_NAME);
        renderPolling = new KafkaTestPolling(renderConsumer, renderTopics);

        eventsPolling.startPolling();
        renderPolling.startPolling();

        eventEmitter = new KafkaTestEmitter(producer, TestConstants.EVENTS_TOPIC_NAME);
        renderEmitter = new KafkaTestEmitter(producer, TestConstants.RENDER_TOPIC_PREFIX);

        testCassandra.deleteMessages();
        testCassandra.deleteDesigns();
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

    @NotNull
    public KafkaProducerConfig createProducerConfig(String clientId) {
        return KafkaProducerConfig.builder()
                .withBootstrapServers(scenario.getKafkaHost() + ":" + scenario.getKafkaPort())
                .withKeySerializer("org.apache.kafka.common.serialization.StringSerializer")
                .withValueSerializer("org.apache.kafka.common.serialization.StringSerializer")
                .withClientId(clientId)
                .withKafkaAcks("1")
                .build();
    }

    public void shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage(OutputMessage designInsertRequestedMessage) {
        final DesignInsertRequested designInsertRequested = Json.decodeValue(designInsertRequestedMessage.getValue().getData(), DesignInsertRequested.class);

        final TilesBitmap bitmap = TilesBitmap.empty();

        final UUID designId = designInsertRequested.getDesignId();

        System.out.println("designId = " + designId);

        eventsPolling.clearMessages();
        renderPolling.clearMessages();

        eventEmitter.send(designInsertRequestedMessage);

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
                    final List<Row> rows = testCassandra.fetchDesigns(designId);
                    assertThat(rows).hasSize(1);
                    TestAssertions.assertExpectedDesign(rows.get(0), TestConstants.JSON_1, "CREATED", bitmap.getBitmap().array());
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignAggregateUpdateRequestedMessage(messages.get(0), designId);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_COMPLETED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(messages.get(0), designId, TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED");
                });

        await().atMost(Duration.ofSeconds(20))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(designId.toString()));
                    assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                    List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                    assertThat(events).hasSize(messages.size());
                    events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(event, designId, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                });
    }

    public void shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage(OutputMessage designInsertRequestedMessage, OutputMessage designUpdateRequestedMessage) {
        final DesignInsertRequested designInsertRequested = Json.decodeValue(designInsertRequestedMessage.getValue().getData(), DesignInsertRequested.class);

        final TilesBitmap bitmap = TilesBitmap.empty();

        final UUID designId = designInsertRequested.getDesignId();

        System.out.println("designId = " + designId);

        eventsPolling.clearMessages();
        renderPolling.clearMessages();

        eventEmitter.send(designInsertRequestedMessage);

        await().atMost(Duration.ofSeconds(60))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(designId.toString()));
                    assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                    List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                    assertThat(events).hasSize(messages.size());
                    events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(event, designId, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                });

        eventsPolling.clearMessages();
        renderPolling.clearMessages();

        eventEmitter.send(designUpdateRequestedMessage);

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_UPDATE_REQUESTED);
                    assertThat(messages).hasSize(1);
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
                    final List<Row> rows = testCassandra.fetchDesigns(designId);
                    assertThat(rows).hasSize(1);
                    TestAssertions.assertExpectedDesign(rows.get(0), TestConstants.JSON_2, "UPDATED", bitmap.getBitmap().array());
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignAggregateUpdateRequestedMessage(messages.get(0), designId);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_COMPLETED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(messages.get(0), designId, TestConstants.JSON_2, TestConstants.CHECKSUM_2, "UPDATED");
                });

        await().atMost(Duration.ofSeconds(20))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(designId.toString()));
                    assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                    List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_2);
                    assertThat(events).hasSize(messages.size());
                    events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(event, designId, TestConstants.JSON_2, TestConstants.CHECKSUM_2));
                });
    }

    public void shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage(OutputMessage designInsertRequestedMessage, OutputMessage designDeleteRequestedMessage) {
        final DesignInsertRequested designInsertRequested = Json.decodeValue(designInsertRequestedMessage.getValue().getData(), DesignInsertRequested.class);

        final TilesBitmap bitmap = TilesBitmap.empty();

        final UUID designId = designInsertRequested.getDesignId();

        System.out.println("designId = " + designId);

        eventsPolling.clearMessages();
        renderPolling.clearMessages();

        eventEmitter.send(designInsertRequestedMessage);

        await().atMost(Duration.ofSeconds(60))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(designId.toString()));
                    assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                    List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                    assertThat(events).hasSize(messages.size());
                    events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(event, designId, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                });

        eventsPolling.clearMessages();
        renderPolling.clearMessages();

        eventEmitter.send(designDeleteRequestedMessage);

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DELETE_REQUESTED);
                    assertThat(messages).hasSize(1);
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
                    final List<Row> rows = testCassandra.fetchDesigns(designId);
                    assertThat(rows).hasSize(1);
                    TestAssertions.assertExpectedDesign(rows.get(0), TestConstants.JSON_1, "DELETED", bitmap.getBitmap().array());
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignAggregateUpdateRequestedMessage(messages.get(0), designId);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_COMPLETED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(messages.get(0), designId, TestConstants.JSON_1, TestConstants.CHECKSUM_1, "DELETED");
                });

        await().atMost(Duration.ofSeconds(20))
                .pollInterval(ONE_SECOND)
                .pollDelay(TEN_SECONDS)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(designId.toString()));
                    assertThat(messages).hasSize(0);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DOCUMENT_DELETED_REQUESTED);
                    assertThat(messages).hasSize(1);
                    messages.forEach(message -> TestAssertions.assertExpectedDesignDocumentDeleteRequestedMessage(message, designId));
                });
    }

    public void shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage(OutputMessage designInsertRequestedMessage, List<OutputMessage> tileRenderCompletedMessages) {
        final DesignInsertRequested designInsertRequested = Json.decodeValue(designInsertRequestedMessage.getValue().getData(), DesignInsertRequested.class);

        final TilesBitmap bitmap = TilesBitmap.empty();

        final UUID designId = designInsertRequested.getDesignId();

        System.out.println("designId = " + designId);

        eventsPolling.clearMessages();
        renderPolling.clearMessages();

        eventEmitter.send(designInsertRequestedMessage);

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
                    final List<Row> rows = testCassandra.fetchDesigns(designId);
                    assertThat(rows).hasSize(1);
                    TestAssertions.assertExpectedDesign(rows.get(0), TestConstants.JSON_1, "CREATED", bitmap.getBitmap().array());
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignAggregateUpdateRequestedMessage(messages.get(0), designId);
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_UPDATE_COMPLETED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignAggregateUpdateCompletedMessage(messages.get(0), designId, TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED");
                });

        await().atMost(Duration.ofSeconds(20))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(designId.toString()));
                    assertThat(messages).hasSize(TestUtils.totalTilesByLevels(TestConstants.LEVELS));
                    List<TileRenderRequested> events = TestUtils.extractTileRenderRequestedEvents(messages, TestConstants.CHECKSUM_1);
                    assertThat(events).hasSize(messages.size());
                    events.forEach(event -> TestAssertions.assertExpectedTileRenderRequestedEvent(event, designId, TestConstants.JSON_1, TestConstants.CHECKSUM_1));
                });

        final OutputMessage tileRenderCompletedMessage1 = tileRenderCompletedMessages.get(0);
        final OutputMessage tileRenderCompletedMessage2 = tileRenderCompletedMessages.get(1);
        final OutputMessage tileRenderCompletedMessage3 = tileRenderCompletedMessages.get(2);
        final OutputMessage tileRenderCompletedMessage4 = tileRenderCompletedMessages.get(3);
        final OutputMessage tileRenderCompletedMessage5 = tileRenderCompletedMessages.get(4);

        renderEmitter.send(tileRenderCompletedMessage1, TestConstants.RENDER_TOPIC_PREFIX + "-completed-0");
        renderEmitter.send(tileRenderCompletedMessage2, TestConstants.RENDER_TOPIC_PREFIX + "-completed-1");
        renderEmitter.send(tileRenderCompletedMessage3, TestConstants.RENDER_TOPIC_PREFIX + "-completed-1");
        renderEmitter.send(tileRenderCompletedMessage4, TestConstants.RENDER_TOPIC_PREFIX + "-completed-2");
        renderEmitter.send(tileRenderCompletedMessage5, TestConstants.RENDER_TOPIC_PREFIX + "-completed-3");

        final TileRenderCompleted tileRenderCompleted1 = Json.decodeValue(tileRenderCompletedMessage1.getValue().getData(), TileRenderCompleted.class);
        final TileRenderCompleted tileRenderCompleted2 = Json.decodeValue(tileRenderCompletedMessage2.getValue().getData(), TileRenderCompleted.class);
        final TileRenderCompleted tileRenderCompleted3 = Json.decodeValue(tileRenderCompletedMessage3.getValue().getData(), TileRenderCompleted.class);
        final TileRenderCompleted tileRenderCompleted4 = Json.decodeValue(tileRenderCompletedMessage4.getValue().getData(), TileRenderCompleted.class);
        final TileRenderCompleted tileRenderCompleted5 = Json.decodeValue(tileRenderCompletedMessage5.getValue().getData(), TileRenderCompleted.class);

        await().atMost(ONE_MINUTE)
                .pollInterval(TEN_SECONDS)
                .untilAsserted(() -> {
                    final List<InputMessage> messages1 = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_TILES_UPDATE_REQUESTED);
                    assertThat(messages1).hasSize(1);
                    messages1.forEach(message -> TestAssertions.assertExpectedDesignAggregateTilesUpdateRequestedMessage(message, designId));
                    final List<InputMessage> messages2 = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_AGGREGATE_TILES_UPDATE_COMPLETED);
                    assertThat(messages2).hasSize(1);
                    messages2.forEach(message -> TestAssertions.assertExpectedDesignAggregateTilesUpdateCompletedMessage(message, designId));
                });

        await().atMost(ONE_MINUTE)
                .pollInterval(TEN_SECONDS)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    messages.forEach(message -> TestAssertions.assertExpectedDesignDocumentUpdateRequestedMessage(message, designId, TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED"));
                });

        bitmap.putTile(tileRenderCompleted1.getLevel(), tileRenderCompleted1.getRow(), tileRenderCompleted1.getCol());
        bitmap.putTile(tileRenderCompleted2.getLevel(), tileRenderCompleted2.getRow(), tileRenderCompleted2.getCol());
        bitmap.putTile(tileRenderCompleted3.getLevel(), tileRenderCompleted3.getRow(), tileRenderCompleted3.getCol());
        bitmap.putTile(tileRenderCompleted4.getLevel(), tileRenderCompleted4.getRow(), tileRenderCompleted4.getCol());

        await().atMost(ONE_MINUTE)
                .pollInterval(TEN_SECONDS)
                .untilAsserted(() -> {
                    final List<Row> rows = testCassandra.fetchDesigns(designId);
                    assertThat(rows).hasSize(1);
                    TestAssertions.assertExpectedDesign(rows.get(0), TestConstants.JSON_1, "CREATED", bitmap.getBitmap().array());
                });
    }
}
