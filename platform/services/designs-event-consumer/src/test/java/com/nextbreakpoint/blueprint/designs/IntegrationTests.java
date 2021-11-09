package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jayway.restassured.RestAssured;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedEvent;
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

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, JSON_1, 3);

            final Message designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            safelyClearEventMessages();
            safelyClearRenderMessages();

            sendMessage(designInsertRequestedMessage);

            await().atMost(ONE_MINUTE)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchMessages(designId);
                        assertThat(rows).hasSize(1);
                        assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                    });

            await().atMost(ONE_MINUTE)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        assertExpectedDesign(rows.get(0), JSON_1, "CREATED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), JSON_1, CHECKSUM_1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(3));
                        messages.forEach(message -> assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindRenderMessages(CHECKSUM_1, MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(3));
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

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, JSON_1, 3);

            final DesignUpdateRequested designUpdateRequested = new DesignUpdateRequested(Uuids.timeBased(), designId, JSON_2, 3);

            final Message designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            final Message designUpdateRequestedMessage = createDesignUpdateRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designUpdateRequested);

            safelyClearEventMessages();
            safelyClearRenderMessages();

            sendMessage(designInsertRequestedMessage);
            sendMessage(designUpdateRequestedMessage);

            await().atMost(ONE_MINUTE)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchMessages(designId);
                        assertThat(rows).hasSize(2);
                        final Set<UUID> uuids = extractUuids(rows);
                        assertThat(uuids).contains(designId);
                        assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                        assertExpectedMessage(rows.get(1), designUpdateRequestedMessage);
                    });

            await().atMost(ONE_MINUTE)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        assertExpectedDesign(rows.get(0), JSON_2, "UPDATED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), JSON_1, CHECKSUM_1);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(1), JSON_2, CHECKSUM_2);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(3) * 2);
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
                        final List<Message> messages = safelyFindRenderMessages(CHECKSUM_2, MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(3));
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

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, JSON_1, 3);

            final DesignDeleteRequested designDeleteRequested = new DesignDeleteRequested(Uuids.timeBased(), designId);

            final Message designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            final Message designDeleteRequestedMessage = createDesignDeleteRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designDeleteRequested);

            safelyClearEventMessages();
            safelyClearRenderMessages();

            sendMessage(designInsertRequestedMessage);
            sendMessage(designDeleteRequestedMessage);

            await().atMost(ONE_MINUTE)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchMessages(designId);
                        assertThat(rows).hasSize(2);
                        final Set<UUID> uuids = extractUuids(rows);
                        assertThat(uuids).contains(designId);
                        assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                        assertExpectedMessage(rows.get(1), designDeleteRequestedMessage);
                    });

            await().atMost(ONE_MINUTE)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        assertExpectedDesign(rows.get(0), JSON_1, "DELETED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(2);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(1), JSON_1, CHECKSUM_1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(3));
                        messages.forEach(message -> assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindRenderMessages(CHECKSUM_1, MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(3));
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

            final UUID evid = Uuids.timeBased();

            System.out.println("designId = " + designId);

            System.out.println("evid = " + evid);

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, JSON_1, 3);

            final TileRenderCompleted tileRenderCompleted1 = new TileRenderCompleted(Uuids.timeBased(), designId, evid, CHECKSUM_1, 0, 0, 0, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted2 = new TileRenderCompleted(Uuids.timeBased(), designId, evid, CHECKSUM_1, 0, 1, 0, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted3 = new TileRenderCompleted(Uuids.timeBased(), designId, evid, CHECKSUM_1, 0, 2, 1, "COMPLETED");
            final TileRenderCompleted tileRenderCompleted4 = new TileRenderCompleted(Uuids.timeBased(), designId, evid, CHECKSUM_1, 0, 3, 1, "COMPLETED");

            final Message designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            final Message tileRenderCompletedMessage1 = createTileRenderCompletedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), tileRenderCompleted1);
            final Message tileRenderCompletedMessage2 = createTileRenderCompletedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), tileRenderCompleted2);
            final Message tileRenderCompletedMessage3 = createTileRenderCompletedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), tileRenderCompleted3);
            final Message tileRenderCompletedMessage4 = createTileRenderCompletedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), tileRenderCompleted4);

            safelyClearEventMessages();
            safelyClearRenderMessages();

            sendMessage(designInsertRequestedMessage);

            await().atMost(ONE_MINUTE)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchMessages(designId);
                        assertThat(rows).hasSize(1);
                        final Set<UUID> uuids = extractUuids(rows);
                        assertThat(uuids).contains(designId);
                        assertExpectedMessage(rows.get(0), designInsertRequestedMessage);
                    });

            await().atMost(ONE_MINUTE)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        assertExpectedDesign(rows.get(0), JSON_1, "CREATED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateRequestedMessage(designId, messages.get(0));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, DESIGN_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        assertExpectedDesignAggregateUpdateCompletedMessage(designId, messages.get(0), JSON_1, CHECKSUM_1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(3));
                        messages.forEach(message -> assertExpectedTileRenderRequestedMessage(message, designId.toString()));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindRenderMessages(CHECKSUM_1, MESSAGE_SOURCE, TILE_RENDER_REQUESTED);
                        assertThat(messages).hasSize(totalTilesByLevels(3));
                        List<TileRenderRequested> events = extractTileRenderRequestedEvents(messages, CHECKSUM_1);
                        assertThat(events).hasSize(messages.size());
                        events.forEach(event -> assertExpectedTileRenderRequestedEvent(designId, event, JSON_1, CHECKSUM_1));
                    });

            sendMessage(tileRenderCompletedMessage1);
            sendMessage(tileRenderCompletedMessage2);
            sendMessage(tileRenderCompletedMessage3);
            sendMessage(tileRenderCompletedMessage4);

            await().atMost(TWO_MINUTES)
                    .pollInterval(TEN_SECONDS)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_AGGREGATE_UPDATE_REQUIRED);
                        assertThat(messages).hasSize(4);
                        messages.forEach(message -> assertExpectedTileAggregateUpdateRequiredMessage(designId, message));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(1);
                        assertExpectedTileAggregateUpdateRequestedMessage(designId, messages.get(0));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindEventMessages(designId.toString(), MESSAGE_SOURCE, TILE_AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        assertExpectedTileAggregateUpdateCompletedMessage(designId, messages.get(0));
                    });
        }
    }

    private int totalTilesByLevels(int levels) {
        return IntStream.range(0, levels).map(level -> (int) Math.rint(Math.pow(2, level * 2))).sum();
    }

    private void sendMessage(Message message) {
        producer.rxSend(createKafkaRecord(message))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    @NotNull
    private List<TileRenderRequested> extractTileRenderRequestedEvents(List<Message> messages, String checksum) {
        return messages.stream()
                .map(message -> Json.decodeValue(message.getBody(), TileRenderRequested.class))
                .filter(event -> event.getChecksum().equals(checksum))
                .collect(Collectors.toList());
    }

    @NotNull
    private static KafkaProducerRecord<String, String> createKafkaRecord(Message message) {
        return KafkaProducerRecord.create(EVENTS_TOPIC_NAME, message.getPartitionKey(), Json.encode(message));
    }

    @NotNull
    private static Message createDesignInsertRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignInsertRequested event) {
        return new Message(messageId.toString(), DESIGN_INSERT_REQUESTED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    @NotNull
    private static Message createDesignUpdateRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignUpdateRequested event) {
        return new Message(messageId.toString(), DESIGN_UPDATE_REQUESTED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    @NotNull
    private static Message createDesignDeleteRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignDeleteRequested event) {
        return new Message(messageId.toString(), DESIGN_DELETE_REQUESTED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    @NotNull
    private static Message createDesignAggregateUpdateRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChangedEvent event) {
        return new Message(messageId.toString(), DESIGN_AGGREGATE_UPDATE_REQUESTED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    @NotNull
    private static Message createDesignAggregateUpdateCompletedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChangedEvent event) {
        return new Message(messageId.toString(), DESIGN_AGGREGATE_UPDATE_COMPLETED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    @NotNull
    private static Message createTileRenderRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, TileRenderCompleted event) {
        return new Message(messageId.toString(), TILE_RENDER_REQUESTED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    @NotNull
    private static Message createTileRenderCompletedMessage(UUID messageId, UUID partitionKey, long timestamp, TileRenderCompleted event) {
        return new Message(messageId.toString(), TILE_RENDER_COMPLETED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    @NotNull
    private static List<Message> safelyFindEventMessages(String partitionKey, String messageSource, String messageType) {
        synchronized (eventMessages) {
            return eventMessages.stream()
                    .map(record -> Json.decodeValue(record.value(), Message.class))
                    .filter(message -> message.getPartitionKey().equals(partitionKey))
                    .filter(message -> message.getSource().equals(messageSource))
                    .filter(message -> message.getType().equals(messageType))
//                    .sorted(Comparator.comparing(Message::getTimestamp))
                    .collect(Collectors.toList());
        }
    }

    @NotNull
    private static List<Message> safelyFindRenderMessages(String partitionKey, String messageSource, String messageType) {
        synchronized (renderMessages) {
            return renderMessages.stream()
                    .map(record -> Json.decodeValue(record.value(), Message.class))
                    .filter(message -> message.getPartitionKey().startsWith(partitionKey))
                    .filter(message -> message.getSource().equals(messageSource))
                    .filter(message -> message.getType().equals(messageType))
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
                .map(row -> row.getString("MESSAGE_PARTITIONKEY"))
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @NotNull
    private List<Row> fetchMessages(UUID designId) {
        return session.rxPrepare("SELECT * FROM MESSAGE WHERE MESSAGE_PARTITIONKEY = ?")
                .map(stmt -> stmt.bind(designId.toString()))
                .flatMap(session::rxExecuteWithFullFetch)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    @NotNull
    private List<Row> fetchDesign(UUID designId) {
        return session.rxPrepare("SELECT * FROM DESIGN WHERE DESIGN_UUID = ?")
                .map(stmt -> stmt.bind(designId))
                .flatMap(session::rxExecuteWithFullFetch)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    private void assertExpectedMessage(Row row, Message message) {
        String actualType = row.getString("MESSAGE_TYPE");
        String actualBody = row.getString("MESSAGE_BODY");
        String actualUuid = row.getString("MESSAGE_UUID");
        String actualSource = row.getString("MESSAGE_SOURCE");
        String actualPartitionKey = row.getString("MESSAGE_PARTITIONKEY");
        Instant actualTimestamp = row.getInstant("MESSAGE_TIMESTAMP");
        UUID actualEsid = row.getUuid("MESSAGE_ESID");
        assertThat(actualEsid).isNotNull();
        assertThat(actualUuid).isEqualTo(message.getUuid());
        assertThat(actualBody).isEqualTo(message.getBody());
        assertThat(actualType).isEqualTo(message.getType());
        assertThat(actualSource).isEqualTo(message.getSource());
        assertThat(actualPartitionKey).isEqualTo(message.getPartitionKey());
        assertThat(actualTimestamp).isEqualTo(Instant.ofEpochMilli(message.getTimestamp()));
    }

    private void assertExpectedDesign(Row row, String data, String status) {
        String actualJson = row.getString("DESIGN_DATA");
        String actualStatus = row.getString("DESIGN_STATUS");
        String actualChecksum = row.getString("DESIGN_CHECKSUM");
        int actualLevels = row.getInt("DESIGN_LEVELS");
        assertThat(actualJson).isEqualTo(data);
        assertThat(actualStatus).isEqualTo(status);
        assertThat(actualChecksum).isNotNull();
        assertThat(actualLevels).isEqualTo(3);
    }

    private void assertExpectedDesignAggregateUpdateRequestedMessage(UUID designId, Message actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getUuid()).isNotNull();
        assertThat(actualMessage.getType()).isEqualTo(DESIGN_AGGREGATE_UPDATE_REQUESTED);
        DesignAggregateUpdateRequested actualEvent = Json.decodeValue(actualMessage.getBody(), DesignAggregateUpdateRequested.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
    }

    private void assertExpectedDesignAggregateUpdateCompletedMessage(UUID designId, Message actualMessage, String data, String checksum) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getUuid()).isNotNull();
        assertThat(actualMessage.getType()).isEqualTo(DESIGN_AGGREGATE_UPDATE_COMPLETED);
        DesignAggregateUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getBody(), DesignAggregateUpdateCompleted.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
    }

    private void assertExpectedTileRenderRequestedMessage(Message actualMessage, String partitionKey) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getPartitionKey()).isEqualTo(partitionKey);
        assertThat(actualMessage.getUuid()).isNotNull();
        assertThat(actualMessage.getType()).isEqualTo(TILE_RENDER_REQUESTED);
        assertThat(actualMessage.getBody()).isNotNull();
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

    private void assertExpectedTileRenderCompletedMessage(UUID designId, Message actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getUuid()).isNotNull();
        assertThat(actualMessage.getType()).isEqualTo(TILE_RENDER_COMPLETED);
        assertThat(actualMessage.getBody()).isNotNull();
    }

    private void assertExpectedTileRenderCompletedEvent(UUID designId, TileRenderCompleted actualEvent, String checksum) {
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getLevel()).isNotNull();
        assertThat(actualEvent.getRow()).isNotNull();
        assertThat(actualEvent.getCol()).isNotNull();
    }

    private void assertExpectedTileAggregateUpdateRequiredMessage(UUID designId, Message actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getUuid()).isNotNull();
        assertThat(actualMessage.getType()).isEqualTo(TILE_AGGREGATE_UPDATE_REQUIRED);
        TileAggregateUpdateRequired actualEvent = Json.decodeValue(actualMessage.getBody(), TileAggregateUpdateRequired.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
    }

    private void assertExpectedTileAggregateUpdateRequestedMessage(UUID designId, Message actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getUuid()).isNotNull();
        assertThat(actualMessage.getType()).isEqualTo(TILE_AGGREGATE_UPDATE_REQUESTED);
        TileAggregateUpdateRequested actualEvent = Json.decodeValue(actualMessage.getBody(), TileAggregateUpdateRequested.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
    }

    private void assertExpectedTileAggregateUpdateCompletedMessage(UUID designId, Message actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getUuid()).isNotNull();
        assertThat(actualMessage.getType()).isEqualTo(TILE_AGGREGATE_UPDATE_COMPLETED);
        TileAggregateUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getBody(), TileAggregateUpdateCompleted.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
    }
}
