package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jayway.restassured.RestAssured;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedEvent;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import io.vertx.core.json.Json;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.time.Duration;
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

    private static final List<KafkaConsumerRecord<String, String>> eventMessages = new ArrayList<>();
    private static final List<KafkaConsumerRecord<String, String>> renderMessages = new ArrayList<>();

    private static KafkaConsumer<String, String> renderMessagesConsumer;
    private static KafkaConsumer<String, String> eventMessagesConsumer;
    private static KafkaProducer<String, String> producer;
    private static CassandraClient session;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        session = CassandraClientFactory.create(environment, vertx, scenario.createCassandraConfig());

        producer = KafkaClientFactory.createProducer(environment, vertx, scenario.createProducerConfig());

        eventMessagesConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test"));

        renderMessagesConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test-rendering"));

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

        pollEventMessages();
        pollRenderMessages();
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

            final OutputMessage designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            safelyClearEventMessages();
            safelyClearRenderMessages();

            sendMessage(designInsertRequestedMessage);

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
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), JSON_1, CHECKSUM_1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        messages.forEach(message -> assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindRenderMessages(CHECKSUM_1, MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
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

            final OutputMessage designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            final OutputMessage designUpdateRequestedMessage = createDesignUpdateRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designUpdateRequested);

            safelyClearEventMessages();
            safelyClearRenderMessages();

            sendMessage(designInsertRequestedMessage);
            sendMessage(designUpdateRequestedMessage);

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
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), JSON_1, CHECKSUM_1);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(1), JSON_2, CHECKSUM_2);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
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
                        final List<InputMessage> messages = safelyFindRenderMessages(CHECKSUM_2, MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
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

            final OutputMessage designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            final OutputMessage designDeleteRequestedMessage = createDesignDeleteRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designDeleteRequested);

            safelyClearEventMessages();
            safelyClearRenderMessages();

            sendMessage(designInsertRequestedMessage);
            sendMessage(designDeleteRequestedMessage);

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
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(1), JSON_1, CHECKSUM_1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        messages.forEach(message -> assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindRenderMessages(CHECKSUM_1, MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
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

            final OutputMessage designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            safelyClearEventMessages();
            safelyClearRenderMessages();

            sendMessage(designInsertRequestedMessage);

            final long[] esid = new long[1];

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchMessages(designId);
                        assertThat(rows).hasSize(1);
                        final Set<UUID> uuids = extractUuids(rows);
                        assertThat(uuids).contains(designId);
                        esid[0] = rows.get(0).getLong("MESSAGE_OFFSET");
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
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), JSON_1, CHECKSUM_1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        messages.forEach(message -> assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = safelyFindRenderMessages(CHECKSUM_1, MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(LEVELS));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            System.out.println("esid = " + esid[0]);

            final TileRenderCompleted tileRenderCompleted0 = new TileRenderCompleted(Uuids.timeBased(), designId, esid[0], CHECKSUM_1, 0, 0, 0, "FAILED");
            final TileRenderCompleted tileRenderCompleted1 = new TileRenderCompleted(Uuids.timeBased(), designId, esid[0], CHECKSUM_1, 1, 0, 0, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted2 = new TileRenderCompleted(Uuids.timeBased(), designId, esid[0], CHECKSUM_1, 1, 1, 0, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted3 = new TileRenderCompleted(Uuids.timeBased(), designId, esid[0], CHECKSUM_1, 1, 2, 1, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted4 = new TileRenderCompleted(Uuids.timeBased(), designId, esid[0], CHECKSUM_1, 1, LEVELS, 1, "COMPLETED");

            final OutputMessage tileRenderCompletedMessage0 = createTileRenderCompletedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), tileRenderCompleted0);
            final OutputMessage tileRenderCompletedMessage1 = createTileRenderCompletedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), tileRenderCompleted1);
            final OutputMessage tileRenderCompletedMessage2 = createTileRenderCompletedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), tileRenderCompleted2);
            final OutputMessage tileRenderCompletedMessage3 = createTileRenderCompletedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), tileRenderCompleted3);
            final OutputMessage tileRenderCompletedMessage4 = createTileRenderCompletedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), tileRenderCompleted4);

            sendMessage(tileRenderCompletedMessage0);
            sendMessage(tileRenderCompletedMessage1);
            sendMessage(tileRenderCompletedMessage2);
            sendMessage(tileRenderCompletedMessage3);
            sendMessage(tileRenderCompletedMessage4);

            await().atMost(ONE_MINUTE)
                    .pollInterval(TEN_SECONDS)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_AGGREGATE_UPDATE_REQUIRED);
                        assertThat(messages1).hasSize(5);
                        messages1.forEach(message -> assertExpectedTileAggregateUpdateRequiredMessage(designId, message));
                        final List<InputMessage> messages2 = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages2).hasSize(1);
                        messages2.forEach(message -> assertExpectedTileAggregateUpdateRequestedMessage(designId, message));
                        final List<InputMessage> messages3 = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_AGGREGATE_UPDATE_COMPLETED);
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

    @NotNull
    private List<Tiles> convertToTilesList(Map<Integer, Tiles> tiles) {
        return tiles.values().stream()
                .sorted(Comparator.comparing(Tiles::getLevel))
                .collect(Collectors.toList());
    }

    private int totalTilesByLevels(int levels) {
        return IntStream.range(0, levels).map(level -> (int) Math.rint(Math.pow(2, level * 2))).sum();
    }

    private void sendMessage(OutputMessage message) {
        producer.rxSend(createKafkaRecord(message))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    @NotNull
    private List<TileRenderRequested> extractTileRenderRequestedEvents(List<InputMessage> messages, String checksum) {
        return messages.stream()
                .map(message -> Json.decodeValue(message.getValue().getData(), TileRenderRequested.class))
                .filter(event -> event.getChecksum().equals(checksum))
                .collect(Collectors.toList());
    }

    @NotNull
    private static KafkaProducerRecord<String, String> createKafkaRecord(OutputMessage message) {
        return KafkaProducerRecord.create(EVENTS_TOPIC_NAME, message.getKey(), Json.encode(message.getValue()));
    }

    @NotNull
    private static OutputMessage createDesignInsertRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignInsertRequested event) {
        return new OutputMessage(partitionKey.toString(), new Payload(messageId, DESIGN_INSERT_REQUESTED, Json.encode(event), "test"));
    }

    @NotNull
    private static OutputMessage createDesignUpdateRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignUpdateRequested event) {
        return new OutputMessage(partitionKey.toString(), new Payload(messageId, DESIGN_UPDATE_REQUESTED, Json.encode(event), "test"));
    }

    @NotNull
    private static OutputMessage createDesignDeleteRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignDeleteRequested event) {
        return new OutputMessage(partitionKey.toString(), new Payload(messageId, DESIGN_DELETE_REQUESTED, Json.encode(event), "test"));
    }

    @NotNull
    private static OutputMessage createDesignAggregateUpdateRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChangedEvent event) {
        return new OutputMessage(partitionKey.toString(), new Payload(messageId, DESIGN_AGGREGATE_UPDATE_REQUESTED, Json.encode(event), "test"));
    }

    @NotNull
    private static OutputMessage createDesignAggregateUpdateCompletedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChangedEvent event) {
        return new OutputMessage(partitionKey.toString(), new Payload(messageId, DESIGN_AGGREGATE_UPDATE_COMPLETED, Json.encode(event), "test"));
    }

    @NotNull
    private static OutputMessage createTileRenderRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, TileRenderCompleted event) {
        return new OutputMessage(partitionKey.toString(), new Payload(messageId, TILE_RENDER_REQUESTED, Json.encode(event), "test"));
    }

    @NotNull
    private static OutputMessage createTileRenderCompletedMessage(UUID messageId, UUID partitionKey, long timestamp, TileRenderCompleted event) {
        return new OutputMessage(partitionKey.toString(), new Payload(messageId, TILE_RENDER_COMPLETED, Json.encode(event), "test"));
    }

    @NotNull
    private static List<InputMessage> safelyFindEventMessages(String partitionKey, String messageSource, String messageType) {
        synchronized (eventMessages) {
            return eventMessages.stream()
                    .map(record -> new InputMessage(record.key(), record.offset(), Json.decodeValue(record.value(), Payload.class), record.timestamp()))
                    .filter(message -> message.getKey().equals(partitionKey))
                    .filter(message -> message.getValue().getSource().equals(messageSource))
                    .filter(message -> message.getValue().getType().equals(messageType))
//                    .sorted(Comparator.comparing(Message::getTimestamp))
                    .collect(Collectors.toList());
        }
    }

    @NotNull
    private static List<InputMessage> safelyFindRenderMessages(String partitionKey, String messageSource, String messageType) {
        synchronized (renderMessages) {
            return renderMessages.stream()
                    .map(record -> new InputMessage(record.key(), record.offset(), Json.decodeValue(record.value(), Payload.class), record.timestamp()))
                    .filter(message -> message.getKey().startsWith(partitionKey))
                    .filter(message -> message.getValue().getSource().equals(messageSource))
                    .filter(message -> message.getValue().getType().equals(messageType))
//                    .sorted(Comparator.comparing(Message::getTimestamp))
                    .collect(Collectors.toList());
        }
    }

    private static void safelyClearEventMessages() {
        synchronized (eventMessages) {
            eventMessages.clear();
        }
    }

    private static void safelyClearRenderMessages() {
        synchronized (renderMessages) {
            renderMessages.clear();
        }
    }

    private static void safelyAppendEventMessage(KafkaConsumerRecord<String, String> record) {
        synchronized (eventMessages) {
            eventMessages.add(record);
        }
    }

    private static void safelyAppendRenderMessage(KafkaConsumerRecord<String, String> record) {
        synchronized (renderMessages) {
            renderMessages.add(record);
        }
    }

    private static void consumeEventMessages(KafkaConsumerRecords<String, String> consumerRecords) {
        IntStream.range(0, consumerRecords.size())
                .forEach(index -> safelyAppendEventMessage(consumerRecords.recordAt(index)));
    }

    private static void consumeRenderMessages(KafkaConsumerRecords<String, String> consumerRecords) {
        IntStream.range(0, consumerRecords.size())
                .forEach(index -> safelyAppendRenderMessage(consumerRecords.recordAt(index)));
    }

    private static void pollEventMessages() {
        eventMessagesConsumer.rxPoll(Duration.ofSeconds(5))
//                .doOnSuccess(records -> System.out.println("Received " + records.size() + " records"))
                .doOnSuccess(IntegrationTests::consumeEventMessages)
                .flatMap(result -> eventMessagesConsumer.rxCommit())
                .doOnError(Throwable::printStackTrace)
                .doAfterTerminate(IntegrationTests::pollEventMessages)
                .subscribe();
    }

    private static void pollRenderMessages() {
        renderMessagesConsumer.rxPoll(Duration.ofSeconds(5))
//                .doOnSuccess(records -> System.out.println("Received " + records.size() + " records"))
                .doOnSuccess(IntegrationTests::consumeRenderMessages)
                .flatMap(result -> renderMessagesConsumer.rxCommit())
                .doOnError(Throwable::printStackTrace)
                .doAfterTerminate(IntegrationTests::pollRenderMessages)
                .subscribe();
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

    private Tiles convertToTiles(Integer level, UdtValue udtValue) {
        return new Tiles(level, udtValue.getInt("REQUESTED"), udtValue.getSet("COMPLETED", Integer.class), udtValue.getSet("FAILED", Integer.class));
    }

    private Map<Integer, Tiles> createTilesMap(int levels) {
        return IntStream.range(0, levels)
                .mapToObj(level -> new Tiles(level, requestedTiles(level), Collections.emptySet(), Collections.emptySet()))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private int requestedTiles(int level) {
        return (int) Math.rint(Math.pow(2, level * 2));
    }
}
