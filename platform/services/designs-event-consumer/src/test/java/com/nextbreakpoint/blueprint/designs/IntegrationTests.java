package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.UdtValue;
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
import io.vertx.core.json.Json;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;

public class IntegrationTests {
    private static final String JSON_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    private static final String JSON_2 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 100] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    private static final String DESIGN_INSERT_REQUESTED = "design-insert-requested";
    private static final String DESIGN_UPDATE_REQUESTED = "design-update-requested";
    private static final String DESIGN_DELETE_REQUESTED = "design-delete-requested";
    private static final String DESIGN_AGGREGATE_UPDATE_REQUESTED = "design-aggregate-update-requested";
    private static final String DESIGN_AGGREGATE_UPDATE_COMPLETED = "design-aggregate-update-completed";
    private static final String TILE_AGGREGATE_UPDATE_REQUIRED = "tile-aggregate-update-required";
    private static final String TILE_AGGREGATE_UPDATE_REQUESTED = "tile-aggregate-update-requested";
    private static final String TILE_AGGREGATE_UPDATE_COMPLETED = "tile-aggregate-update-completed";
    private static final String TILE_RENDER_REQUESTED = "tile-render-requested";
    private static final String TILE_RENDER_COMPLETED = "tile-render-completed";
    private static final String MESSAGE_SOURCE = "service-designs";
    private static final String EVENTS_TOPIC_NAME = "design-event";
    private static final String RENDERING_QUEUE_TOPIC_NAME = "tiles-rendering-queue";
    private static final String CHECKSUM_1 = Checksum.of(JSON_1);
    private static final String CHECKSUM_2 = Checksum.of(JSON_2);
    private static final int LEVELS = 3;

    private static final Environment environment = Environment.getDefaultEnvironment();

    private static final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private static final TestScenario scenario = new TestScenario();

    private static CassandraClient session;

    private static KafkaTestPolling eventMessagesPolling;
    private static KafkaTestPolling renderMessagesPolling;
    private static KafkaTestEmitter eventMessageEmitter;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        session = CassandraClientFactory.create(environment, vertx, scenario.createCassandraConfig());

        KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, scenario.createProducerConfig());

        KafkaConsumer<String, String> eventMessagesConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test"));

        KafkaConsumer<String, String> renderMessagesConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test-rendering"));

        eventMessagesConsumer.rxSubscribe(Collections.singleton(EVENTS_TOPIC_NAME))
//                .flatMap(ignore -> eventMessagesConsumer.rxPoll(Duration.ofSeconds(5)))
//                .flatMap(records -> eventMessagesConsumer.rxAssignment())
//                .flatMap(partitions -> eventMessagesConsumer.rxSeekToEnd(partitions))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();

        renderMessagesConsumer.rxSubscribe(Collections.singleton(RENDERING_QUEUE_TOPIC_NAME))
//                .flatMap(ignore -> renderMessagesConsumer.rxPoll(Duration.ofSeconds(5)))
//                .flatMap(records -> renderMessagesConsumer.rxAssignment())
//                .flatMap(partitions -> renderMessagesConsumer.rxSeekToEnd(partitions))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();

        eventMessagesPolling = new KafkaTestPolling(eventMessagesConsumer);
        renderMessagesPolling = new KafkaTestPolling(renderMessagesConsumer);

        eventMessagesPolling.startPolling();
        renderMessagesPolling.startPolling();

        eventMessageEmitter = new KafkaTestEmitter(producer, EVENTS_TOPIC_NAME);
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

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, JSON_1, LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            eventMessagesPolling.clearMessages();
            renderMessagesPolling.clearMessages();

            eventMessageEmitter.sendMessage(designInsertRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), "test", DESIGN_INSERT_REQUESTED);
                        assertThat(messages).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchMessages(designId);
                        assertThat(rows).hasSize(1);
                        assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        final List<Tiles> tiles = convertToTilesList(createTilesMap(LEVELS));
                        assertExpectedDesign(rows.get(0), JSON_1, "CREATED", tiles);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), JSON_1, CHECKSUM_1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        messages.forEach(message -> assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = renderMessagesPolling.findMessages(MESSAGE_SOURCE, TILE_RENDER_REQUESTED, key -> key.startsWith(CHECKSUM_1));
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });
        }

        @Test
        @Order(value = 200)
        @DisplayName("Should update the design after receiving a DesignUpdateRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage() {
            final UUID designId = UUID.randomUUID();

            System.out.println("designId = " + designId);

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, JSON_1, LEVELS);

            final DesignUpdateRequested designUpdateRequested = new DesignUpdateRequested(Uuids.timeBased(), designId, JSON_2, LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            final OutputMessage designUpdateRequestedMessage = new DesignUpdateRequestedOutputMapper("test").transform(designUpdateRequested);

            eventMessagesPolling.clearMessages();
            renderMessagesPolling.clearMessages();

            eventMessageEmitter.sendMessage(designInsertRequestedMessage);
            eventMessageEmitter.sendMessage(designUpdateRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = eventMessagesPolling.findMessages(designId.toString(), "test", DESIGN_INSERT_REQUESTED);
                        assertThat(messages1).hasSize(1);
                        final List<InputMessage> messages2 = eventMessagesPolling.findMessages(designId.toString(), "test", DESIGN_UPDATE_REQUESTED);
                        assertThat(messages2).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchMessages(designId);
                        assertThat(rows).hasSize(2);
                        final Set<UUID> uuids = extractUuids(rows);
                        assertThat(uuids).contains(designId);
                        assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                        assertExpectedMessage(rows.get(1), designUpdateRequestedMessage);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        final List<Tiles> tiles = convertToTilesList(createTilesMap(LEVELS));
                        assertExpectedDesign(rows.get(0), JSON_2, "UPDATED", tiles);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), JSON_1, CHECKSUM_1);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(1), JSON_2, CHECKSUM_2);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS) * 2);
                        messages.forEach(message -> assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events1 = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        List<TileRenderRequested> events2 = extractTileRenderRequestedEvents(messages, CHECKSUM_2);
                        assertThat(events1).hasSize(messages.size() / 2);
                        assertThat(events2).hasSize(messages.size() / 2);
                        events1.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                        events2.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_2, CHECKSUM_2));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = renderMessagesPolling.findMessages(MESSAGE_SOURCE, TILE_RENDER_REQUESTED, key -> key.startsWith(CHECKSUM_2));
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_2);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_2, CHECKSUM_2));
                    });
        }

        @Test
        @Order(value = 300)
        @DisplayName("Should update the design after receiving a DesignDeleteRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage() {
            final UUID designId = UUID.randomUUID();

            System.out.println("designId = " + designId);

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, JSON_1, LEVELS);

            final DesignDeleteRequested designDeleteRequested = new DesignDeleteRequested(Uuids.timeBased(), designId);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            final OutputMessage designDeleteRequestedMessage = new DesignDeleteRequestedOutputMapper("test").transform(designDeleteRequested);

            eventMessagesPolling.clearMessages();
            renderMessagesPolling.clearMessages();

            eventMessageEmitter.sendMessage(designInsertRequestedMessage);
            eventMessageEmitter.sendMessage(designDeleteRequestedMessage);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = eventMessagesPolling.findMessages(designId.toString(), "test", DESIGN_INSERT_REQUESTED);
                        assertThat(messages1).hasSize(1);
                        final List<InputMessage> messages2 = eventMessagesPolling.findMessages(designId.toString(), "test", DESIGN_DELETE_REQUESTED);
                        assertThat(messages2).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchMessages(designId);
                        assertThat(rows).hasSize(2);
                        final Set<UUID> uuids = extractUuids(rows);
                        assertThat(uuids).contains(designId);
                        assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                        assertExpectedMessage(rows.get(1), designDeleteRequestedMessage);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        final List<Tiles> tiles = convertToTilesList(createTilesMap(LEVELS));
                        assertExpectedDesign(rows.get(0), JSON_1, "DELETED", tiles);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(1), JSON_1, CHECKSUM_1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        messages.forEach(message -> assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = renderMessagesPolling.findMessages(MESSAGE_SOURCE, TILE_RENDER_REQUESTED, key -> key.startsWith(CHECKSUM_1));
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });
        }

        @Test
        @Order(value = 400)
        @DisplayName("Should update the design after receiving a TileRenderCompleted event")
        public void shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage() {
            final UUID designId = UUID.randomUUID();

            System.out.println("designId = " + designId);

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, JSON_1, LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            eventMessagesPolling.clearMessages();
            renderMessagesPolling.clearMessages();

            eventMessageEmitter.sendMessage(designInsertRequestedMessage);

            final long[] offset = new long[1];

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), "test", DESIGN_INSERT_REQUESTED);
                        assertThat(messages).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchMessages(designId);
                        assertThat(rows).hasSize(1);
                        final Set<UUID> uuids = extractUuids(rows);
                        assertThat(uuids).contains(designId);
                        offset[0] = rows.get(0).getLong("MESSAGE_OFFSET");
                        assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        final List<Tiles> tiles = convertToTilesList(createTilesMap(LEVELS));
                        assertExpectedDesign(rows.get(0), JSON_1, "CREATED", tiles);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), JSON_1, CHECKSUM_1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        messages.forEach(message -> assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = renderMessagesPolling.findMessages(MESSAGE_SOURCE, TILE_RENDER_REQUESTED, key -> key.startsWith(CHECKSUM_1));
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            System.out.println("offset = " + offset[0]);

            final TileRenderCompleted tileRenderCompleted0 = new TileRenderCompleted(Uuids.timeBased(), designId, offset[0], CHECKSUM_1, 0, 0, 0, "FAILED");
            final TileRenderCompleted tileRenderCompleted1 = new TileRenderCompleted(Uuids.timeBased(), designId, offset[0], CHECKSUM_1, 1, 0, 0, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted2 = new TileRenderCompleted(Uuids.timeBased(), designId, offset[0], CHECKSUM_1, 1, 1, 0, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted3 = new TileRenderCompleted(Uuids.timeBased(), designId, offset[0], CHECKSUM_1, 1, 2, 1, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted4 = new TileRenderCompleted(Uuids.timeBased(), designId, offset[0], CHECKSUM_1, 1, 3, 1, "COMPLETED");

            final OutputMessage tileRenderCompletedMessage0 = new TileRenderCompletedOutputMapper("test").transform(tileRenderCompleted0);
            final OutputMessage tileRenderCompletedMessage1 = new TileRenderCompletedOutputMapper("test").transform(tileRenderCompleted1);
            final OutputMessage tileRenderCompletedMessage2 = new TileRenderCompletedOutputMapper("test").transform(tileRenderCompleted2);
            final OutputMessage tileRenderCompletedMessage3 = new TileRenderCompletedOutputMapper("test").transform(tileRenderCompleted3);
            final OutputMessage tileRenderCompletedMessage4 = new TileRenderCompletedOutputMapper("test").transform(tileRenderCompleted4);

            eventMessageEmitter.sendMessage(tileRenderCompletedMessage0);
            eventMessageEmitter.sendMessage(tileRenderCompletedMessage1);
            eventMessageEmitter.sendMessage(tileRenderCompletedMessage2);
            eventMessageEmitter.sendMessage(tileRenderCompletedMessage3);
            eventMessageEmitter.sendMessage(tileRenderCompletedMessage4);

            await().atMost(ONE_MINUTE)
                    .pollInterval(TEN_SECONDS)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, TILE_AGGREGATE_UPDATE_REQUIRED);
                        assertThat(messages1).hasSize(5);
                        messages1.forEach(message -> assertExpectedTileAggregateUpdateRequiredMessage(designId, message));
                        final List<InputMessage> messages2 = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, TILE_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages2).hasSize(1);
                        messages2.forEach(message -> assertExpectedTileAggregateUpdateRequestedMessage(designId, message));
                        final List<InputMessage> messages3 = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, TILE_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages3).hasSize(1);
                        messages3.forEach(message -> assertExpectedTileAggregateUpdateCompletedMessage(designId, message));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        final Map<Integer, Tiles> tilesMap = createTilesMap(LEVELS);
                        tilesMap.put(0, new Tiles(0, 1, Set.of(), Set.of(0)));
                        tilesMap.put(1, new Tiles(1, 4, Set.of(0, 65536, 131073, 196609), Set.of()));
                        final List<Tiles> tiles = convertToTilesList(tilesMap);
                        assertExpectedDesign(rows.get(0), JSON_1, "CREATED", tiles);
                    });
        }
    }

    private int totalTilesByLevels(int levels) {
        return IntStream.range(0, levels).map(this::totalTileByLevel).sum();
    }

    private int totalTileByLevel(int level) {
        return (int) Math.rint(Math.pow(2, level * 2));
    }

    @NotNull
    private List<Tiles> convertToTilesList(Map<Integer, Tiles> tiles) {
        return tiles.values().stream()
                .sorted(Comparator.comparing(Tiles::getLevel))
                .collect(Collectors.toList());
    }

    @NotNull
    private Tiles convertToTiles(Integer level, UdtValue udtValue) {
        return new Tiles(level, udtValue.getInt("REQUESTED"), udtValue.getSet("COMPLETED", Integer.class), udtValue.getSet("FAILED", Integer.class));
    }

    @NotNull
    private Map<Integer, Tiles> createTilesMap(int levels) {
        return IntStream.range(0, levels)
                .mapToObj(level -> new Tiles(level, totalTileByLevel(level), Collections.emptySet(), Collections.emptySet()))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    @NotNull
    private List<TileRenderRequested> extractTileRenderRequestedEvents(List<InputMessage> messages, String checksum) {
        return messages.stream()
                .map(message -> Json.decodeValue(message.getValue().getData(), TileRenderRequested.class))
                .filter(event -> event.getChecksum().equals(checksum))
                .collect(Collectors.toList());
    }

    @NotNull
    private Set<UUID> extractUuids(List<Row> rows) {
        return rows.stream()
                .map(row -> row.getString("MESSAGE_KEY"))
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @NotNull
    private List<Row> fetchMessages(UUID designId) {
        return session.rxPrepare("SELECT * FROM MESSAGE WHERE MESSAGE_KEY = ?")
                .map(stmt -> stmt.bind(designId.toString()).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecuteWithFullFetch)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    @NotNull
    private List<Row> fetchDesign(UUID designId) {
        return session.rxPrepare("SELECT * FROM DESIGN WHERE DESIGN_UUID = ?")
                .map(stmt -> stmt.bind(designId).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecuteWithFullFetch)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    private void assertExpectedMessage(Row row, OutputMessage message) {
        String actualType = row.getString("MESSAGE_TYPE");
        String actualValue = row.getString("MESSAGE_VALUE");
        UUID actualUuid = row.getUuid("MESSAGE_UUID");
        String actualSource = row.getString("MESSAGE_SOURCE");
        String actualKey = row.getString("MESSAGE_KEY");
        Instant actualTimestamp = row.getInstant("MESSAGE_TIMESTAMP");
        Long actualOffset = row.getLong("MESSAGE_OFFSET");
        assertThat(actualOffset).isNotNull();
        assertThat(actualUuid).isEqualTo(message.getValue().getUuid());
        assertThat(actualValue).isEqualTo(message.getValue().getData());
        assertThat(actualType).isEqualTo(message.getValue().getType());
        assertThat(actualSource).isEqualTo(message.getValue().getSource());
        assertThat(actualKey).isEqualTo(message.getKey());
        assertThat(actualTimestamp).isNotNull();
    }

    private void assertExpectedDesign(Row row, String data, String status, List<Tiles> tiles) {
        String actualJson = row.getString("DESIGN_DATA");
        String actualStatus = row.getString("DESIGN_STATUS");
        String actualChecksum = row.getString("DESIGN_CHECKSUM");
        int actualLevels = row.getInt("DESIGN_LEVELS");
        final Map<Integer, UdtValue> tilesMap = row.getMap("DESIGN_TILES", Integer.class, UdtValue.class);
        final List<Tiles> actualTiles = tilesMap.entrySet().stream()
                .map(entry -> convertToTiles(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        assertThat(actualJson).isEqualTo(data);
        assertThat(actualStatus).isEqualTo(status);
        assertThat(actualChecksum).isNotNull();
        assertThat(actualLevels).isEqualTo(LEVELS);
        assertThat(actualTiles).isEqualTo(tiles);
    }

    private void assertExpectedDesignAggregateUpdateRequestedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(DESIGN_AGGREGATE_UPDATE_REQUESTED);
        DesignAggregateUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignAggregateUpdateRequested.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
    }

    private void assertExpectedDesignAggregateUpdateCompletedMessage(UUID designId, InputMessage actualMessage, String data, String checksum) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(DESIGN_AGGREGATE_UPDATE_COMPLETED);
        DesignAggregateUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignAggregateUpdateCompleted.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
    }

    private void assertExpectedTileRenderRequestedMessage(InputMessage actualMessage, String partitionKey) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(partitionKey);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TILE_RENDER_REQUESTED);
        assertThat(actualMessage.getValue()).isNotNull();
    }

    private void assertExpectedTileRenderRequestedEvent(UUID designId, TileRenderRequested actualEvent, String data, String checksum) {
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getLevel()).isNotNull();
        assertThat(actualEvent.getRow()).isNotNull();
        assertThat(actualEvent.getCol()).isNotNull();
    }

    private void assertExpectedTileRenderCompletedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TILE_RENDER_COMPLETED);
        assertThat(actualMessage.getValue()).isNotNull();
    }

    private void assertExpectedTileRenderCompletedEvent(UUID designId, TileRenderCompleted actualEvent, String checksum) {
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getLevel()).isNotNull();
        assertThat(actualEvent.getRow()).isNotNull();
        assertThat(actualEvent.getCol()).isNotNull();
    }

    private void assertExpectedTileAggregateUpdateRequiredMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TILE_AGGREGATE_UPDATE_REQUIRED);
        TileAggregateUpdateRequired actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileAggregateUpdateRequired.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
    }

    private void assertExpectedTileAggregateUpdateRequestedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TILE_AGGREGATE_UPDATE_REQUESTED);
        TileAggregateUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileAggregateUpdateRequested.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
    }

    private void assertExpectedTileAggregateUpdateCompletedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TILE_AGGREGATE_UPDATE_COMPLETED);
        TileAggregateUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileAggregateUpdateCompleted.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
    }
}
