package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
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
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

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
    private static final String AGGREGATE_UPDATE_REQUESTED = "aggregate-update-requested";
    private static final String AGGREGATE_UPDATE_COMPLETED = "aggregate-update-completed";
    private static final String TILE_RENDER_REQUESTED = "tile-render-requested";
    private static final String TILE_RENDER_COMPLETED = "tile-render-completed";
    private static final String MESSAGE_SOURCE = "service-designs";
    private static final String TOPIC_NAME = "design-event";

    private static final Environment environment = Environment.getDefaultEnvironment();

    private static final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private static final TestScenario scenario = new TestScenario();

    private static final List<KafkaConsumerRecord<String, String>> records = new ArrayList<>();

    private static KafkaConsumer<String, String> consumer;
    private static KafkaProducer<String, String> producer;
    private static CassandraClient session;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        session = CassandraClientFactory.create(environment, vertx, scenario.createCassandraConfig());

        producer = KafkaClientFactory.createProducer(environment, vertx, scenario.createProducerConfig());

        consumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test"));

        consumer.rxSubscribe(Collections.singleton(TOPIC_NAME))
                .doOnError(Throwable::printStackTrace)
                .toBlocking()
                .value();

        pollRecords();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        try {
            vertx.rxClose()
                    .doOnError(Throwable::printStackTrace)
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
        @DisplayName("Should update the design after receiving a DesignInsertRequested event")
        public void shouldInsertDesignWhenReceivingAMessage() {
            final UUID designId = UUID.randomUUID();

            final long eventTimestamp = System.currentTimeMillis() - 200;

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(designId, JSON_1, eventTimestamp);

            final Message designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            safelyClearMessages();

            producer.rxSend(createKafkaRecord(designInsertRequestedMessage))
                    .doOnError(Throwable::printStackTrace)
                    .toBlocking()
                    .value();

            final String checksum = Checksum.of(JSON_1);

            await().atMost(ONE_MINUTE)
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
                        assertExpectedDesign(rows.get(0), JSON_1, "CREATED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(designId.toString(), MESSAGE_SOURCE, AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(1);
                        assertExpectedAggregateUpdateRequestedMessage(designId, designInsertRequestedMessage.getTimestamp(), messages.get(0));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(designId.toString(), MESSAGE_SOURCE, AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(1);
                        assertExpectedAggregateUpdateCompletedMessage(designId, designInsertRequestedMessage.getTimestamp(), messages.get(0), JSON_1, checksum);
                    });
        }

        @Test
        @Disabled
        @DisplayName("Should update the design after receiving a DesignUpdateRequested event")
        public void shouldUpdateDesignWhenReceivingAMessage() {
            final UUID designId = UUID.randomUUID();

            final long eventTimestamp1 = System.currentTimeMillis() - 200;

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(designId, JSON_1, eventTimestamp1);

            final long eventTimestamp2 = System.currentTimeMillis() - 100;

            final DesignUpdateRequested designUpdateRequested = new DesignUpdateRequested(designId, JSON_2, eventTimestamp2);

            final Message designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            final Message designUpdateRequestedMessage = createDesignUpdateRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designUpdateRequested);

            safelyClearMessages();

            producer.rxSend(createKafkaRecord(designInsertRequestedMessage))
                    .doOnError(Throwable::printStackTrace)
                    .toBlocking()
                    .value();

            producer.rxSend(createKafkaRecord(designUpdateRequestedMessage))
                    .doOnError(Throwable::printStackTrace)
                    .toBlocking()
                    .value();

            final String checksum1 = Checksum.of(JSON_1);
            final String checksum2 = Checksum.of(JSON_2);

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

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        assertExpectedDesign(rows.get(0), JSON_2, "UPDATED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(designId.toString(), MESSAGE_SOURCE, AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(2);
                        assertExpectedAggregateUpdateRequestedMessage(designId, designInsertRequestedMessage.getTimestamp(), messages.get(0));
                        assertExpectedAggregateUpdateRequestedMessage(designId, designUpdateRequestedMessage.getTimestamp(), messages.get(1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(designId.toString(), MESSAGE_SOURCE, AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(2);
//                        assertExpectedAggregateUpdateCompletedEvent(designId, designInsertRequestedMessage.getTimestamp(), messages.get(0), JSON_1, checksum1);
                        assertExpectedAggregateUpdateCompletedMessage(designId, designUpdateRequestedMessage.getTimestamp(), messages.get(1), JSON_2, checksum2);
                    });
        }

        @Test
        @Disabled
        @DisplayName("Should delete the design after receiving a DesignDeleteRequested event")
        public void shouldDeleteDesignWhenReceivingAMessage() {
            final UUID designId = UUID.randomUUID();

            final long eventTimestamp1 = System.currentTimeMillis() - 200;

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(designId, JSON_1, eventTimestamp1);

            final long eventTimestamp2 = System.currentTimeMillis() - 100;

            final DesignDeleteRequested designDeleteRequested = new DesignDeleteRequested(designId, eventTimestamp2);

            final Message designInsertRequestedMessage = createDesignInsertRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designInsertRequested);

            final Message designDeleteRequestedMessage = createDesignDeleteRequestedMessage(UUID.randomUUID(), designId, System.currentTimeMillis(), designDeleteRequested);

            safelyClearMessages();

            producer.rxSend(createKafkaRecord(designInsertRequestedMessage))
                    .doOnError(Throwable::printStackTrace)
                    .toBlocking()
                    .value();

            producer.rxSend(createKafkaRecord(designDeleteRequestedMessage))
                    .doOnError(Throwable::printStackTrace)
                    .toBlocking()
                    .value();

            final String checksum1 = Checksum.of(JSON_1);
            final String checksum2 = Checksum.of(JSON_1);

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

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = fetchDesign(designId);
                        assertThat(rows).hasSize(1);
                        assertExpectedDesign(rows.get(0), JSON_1, "DELETED");
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(designId.toString(), MESSAGE_SOURCE, AGGREGATE_UPDATE_REQUESTED);
                        assertThat(messages).hasSize(2);
                        assertExpectedAggregateUpdateRequestedMessage(designId, designInsertRequestedMessage.getTimestamp(), messages.get(0));
                        assertExpectedAggregateUpdateRequestedMessage(designId, designDeleteRequestedMessage.getTimestamp(), messages.get(1));
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(designId.toString(), MESSAGE_SOURCE, AGGREGATE_UPDATE_COMPLETED);
                        assertThat(messages).hasSize(2);
//                        assertExpectedAggregateUpdateCompletedEvent(designId, designInsertRequestedMessage.getTimestamp(), messages.get(0), JSON_1, checksum1);
                        assertExpectedAggregateUpdateCompletedMessage(designId, designDeleteRequestedMessage.getTimestamp(), messages.get(1), JSON_1, checksum2);
                    });
        }
   }

    @NotNull
    private static KafkaProducerRecord<String, String> createKafkaRecord(Message message) {
        return KafkaProducerRecord.create(TOPIC_NAME, message.getPartitionKey(), Json.encode(message));
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
        return new Message(messageId.toString(), AGGREGATE_UPDATE_REQUESTED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    @NotNull
    private static Message createDesignAggregateUpdateCompletedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChangedEvent event) {
        return new Message(messageId.toString(), AGGREGATE_UPDATE_COMPLETED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    @NotNull
    private static Message createTileRenderRequestedMessage(UUID messageId, UUID partitionKey, long timestamp, TileRenderCompleted event) {
        return new Message(messageId.toString(), TILE_RENDER_REQUESTED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    @NotNull
    private static Message createTileRenderCompletedMessage(UUID messageId, UUID partitionKey, long timestamp, TileRenderCompleted event) {
        return new Message(messageId.toString(), TILE_RENDER_COMPLETED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    private static void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    @NotNull
    private static List<Message> safelyFindMessages(String partitionKey, String messageSource, String messageType) {
        synchronized (records) {
            return records.stream()
                    .map(record -> Json.decodeValue(record.value(), Message.class))
                    .filter(message -> message.getPartitionKey().equals(partitionKey))
                    .filter(message -> message.getSource().equals(messageSource))
                    .filter(message -> message.getType().equals(messageType))
//                    .sorted(Comparator.comparing(Message::getTimestamp))
                    .collect(Collectors.toList());
        }
    }

    private static void safelyClearMessages() {
        synchronized (records) {
            records.clear();
        }
    }

    private static void safelyAppendRecord(KafkaConsumerRecord<String, String> record) {
        synchronized (records) {
            records.add(record);
        }
    }

    private static void consumeRecords(KafkaConsumerRecords<String, String> consumerRecords) {
        System.out.println("Received " + consumerRecords.size() + " messages");

        IntStream.range(0, consumerRecords.size())
                .forEach(index -> safelyAppendRecord(consumerRecords.recordAt(index)));

        pollRecords();
        commitOffsets();
    }

    private static void pollRecords() {
        consumer.rxPoll(Duration.ofSeconds(5))
                .doOnSuccess(IntegrationTests::consumeRecords)
                .doOnError(Throwable::printStackTrace)
                .subscribe();
    }

    private static void commitOffsets() {
        consumer.rxCommit()
                .doOnError(Throwable::printStackTrace)
                .subscribe();
    }

    @NotNull
    private Set<UUID> extractUuids(List<Row> rows) {
        return rows.stream()
                .map(row -> row.getString("MESSAGE_PARTITIONKEY"))
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @NotNull
    private List<Row> fetchMessages(UUID designId) {
        return session.rxPrepare("SELECT * FROM MESSAGE WHERE MESSAGE_PARTITIONKEY = ?")
                .map(stmt -> stmt.bind(designId.toString()))
                .flatMap(session::rxExecuteWithFullFetch)
                .toBlocking()
                .value();
    }

    @NotNull
    private List<Row> fetchDesign(UUID designId) {
        return session.rxPrepare("SELECT * FROM DESIGN WHERE DESIGN_UUID = ?")
                .map(stmt -> stmt.bind(designId))
                .flatMap(session::rxExecuteWithFullFetch)
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
        UUID actualEvid = row.getUuid("MESSAGE_EVID");
        assertThat(actualEvid).isNotNull();
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
        assertThat(actualJson).isEqualTo(data);
        assertThat(actualStatus).isEqualTo(status);
        assertThat(actualChecksum).isNotNull();
    }

    private void assertExpectedAggregateUpdateRequestedMessage(UUID designId, long eventTimestamp, Message actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getUuid()).isNotNull();
        assertThat(actualMessage.getType()).isEqualTo(AGGREGATE_UPDATE_REQUESTED);
        DesignAggregateUpdateRequested actualEvent = Json.decodeValue(actualMessage.getBody(), DesignAggregateUpdateRequested.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getTimestamp()).isNotNull();
        assertThat(actualEvent.getTimestamp()).isEqualTo(eventTimestamp);
    }

    private void assertExpectedAggregateUpdateCompletedMessage(UUID designId, long eventTimestamp, Message actualMessage, String data, String checksum) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getUuid()).isNotNull();
        assertThat(actualMessage.getType()).isEqualTo(AGGREGATE_UPDATE_COMPLETED);
        DesignAggregateUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getBody(), DesignAggregateUpdateCompleted.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
    }
}
