package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.jayway.restassured.RestAssured;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import com.nextbreakpoint.blueprint.designs.model.*;
import io.vertx.core.json.Json;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;

public class IntegrationTests {
    private static final String JSON_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    private static final String JSON_2 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 100] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";

    private static final TestScenario scenario = new TestScenario();

    private static Environment environment = Environment.getDefaultEnvironment();

    private static final List<ConsumerRecord<String, String>> records = new ArrayList<>();
    private static KafkaConsumer<String, String> consumer;
    private static KafkaProducer<String, String> producer;
    private static CassandraClient session;
    private static Thread polling;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

        session = CassandraClientFactory.create(environment, vertx, scenario.createCassandraConfig());

        producer = KafkaUtils.createProducer(environment, scenario.createProducerConfig());

        consumer = KafkaUtils.createConsumer(environment, scenario.createConsumerConfig("test"));

        consumer.subscribe(Collections.singleton("design-event"));

        polling = createConsumerThread();

        polling.start();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        if (polling != null) {
            try {
                polling.interrupt();
                polling.join();
            } catch (Exception ignore) {
            }
        }

        if (consumer != null) {
            try {
                consumer.close();
            } catch (Exception ignore) {
            }
        }

        if (producer != null) {
            try {
                producer.close();
            } catch (Exception ignore) {
            }
        }

        if (session != null) {
            try {
                session.close();
            } catch (Exception ignore) {
            }
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
        @DisplayName("Should create version, renders and tiles after receiving a design changed event")
        public void shouldCreateVersionAndRendersAndTilesWhenReceivingAMessage() {
            final long eventTimestamp = System.currentTimeMillis();

            final UUID designId = UUID.randomUUID();

            final String checksum = UUID.randomUUID().toString();

            final DesignChanged designChanged = new DesignChanged(designId, JSON_1, checksum, eventTimestamp);

            final long messageTimestamp = System.currentTimeMillis();

            final Message designChangedMessage = createDesignChangedMessage(UUID.randomUUID(), designId, messageTimestamp, designChanged);

            safelyClearMessages();

            producer.send(createKafkaRecord(designChangedMessage));

            final UUID versionId = assertVersionCreated(checksum, JSON_1);

            assertLevelCreated(versionId, JSON_1, checksum, (short) 0);
            assertLevelCreated(versionId, JSON_1, checksum, (short) 1);
            assertLevelCreated(versionId, JSON_1, checksum, (short) 2);

            assertTileCreated(versionId, JSON_1, checksum, (short) 0, (short) 0, (short) 0);

            assertTileCreated(versionId, JSON_1, checksum, (short) 1, (short) 0, (short) 0);
            assertTileCreated(versionId, JSON_1, checksum, (short) 1, (short) 0, (short) 1);
            assertTileCreated(versionId, JSON_1, checksum, (short) 1, (short) 1, (short) 0);
            assertTileCreated(versionId, JSON_1, checksum, (short) 1, (short) 1, (short) 1);

            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 0, (short) 0);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 0, (short) 1);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 0, (short) 2);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 0, (short) 3);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 1, (short) 0);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 1, (short) 1);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 1, (short) 2);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 1, (short) 3);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 2, (short) 0);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 2, (short) 1);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 2, (short) 2);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 2, (short) 3);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 3, (short) 0);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 3, (short) 1);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 3, (short) 2);
            assertTileCreated(versionId, JSON_1, checksum, (short) 2, (short) 3, (short) 3);
        }

        @Test
        @DisplayName("Should complete tiles and render after receiving all tile completed messages")
        public void shouldCompleteTileAndRenderWhenReceivingAMessage() {
            final long eventTimestamp = System.currentTimeMillis();

            final UUID designId = UUID.randomUUID();

            final String checksum = UUID.randomUUID().toString();

            final DesignChanged designChanged = new DesignChanged(designId, JSON_2, checksum, eventTimestamp);

            final long messageTimestamp = System.currentTimeMillis();

//            session.rxPrepare("TRUNCATE TILE_ENTITY")
//                    .map(PreparedStatement::bind)
//                    .flatMap(session::rxExecute)
//                    .toBlocking()
//                    .value();

//            final Single<PreparedStatement> preparedStatementSingle = session.rxPrepare("INSERT INTO TILE_ENTITY (TILE_UUID, VERSION_UUID, TILE_LEVEL, TILE_ROW, TILE_COL, TILE_CREATED, TILE_UPDATED, TILE_PUBLISHED) VALUES (?,?,?,?,?,toTimeStamp(now()),toTimeStamp(now()),toTimeStamp(now()))");

//            final UUID tileId = UUID.randomUUID();

//            preparedStatementSingle
//                    .map(stmt -> stmt.bind(tileId, versionId, level, y, x))
//                    .flatMap(session::rxExecute)
//                    .toBlocking()
//                    .value();

            final Message designChangedMessage = createDesignChangedMessage(UUID.randomUUID(), designId, messageTimestamp, designChanged);

            safelyClearMessages();

            producer.send(createKafkaRecord(designChangedMessage));

            final UUID versionId = assertVersionCreated(checksum, JSON_2);

            assertLevelCreated(versionId, JSON_2, checksum, (short) 0);
            assertLevelCreated(versionId, JSON_2, checksum, (short) 1);
            assertLevelCreated(versionId, JSON_2, checksum, (short) 2);

            assertTileCreated(versionId, JSON_2, checksum, (short) 0, (short) 0, (short) 0);

            assertTileCreated(versionId, JSON_2, checksum, (short) 1, (short) 0, (short) 0);
            assertTileCreated(versionId, JSON_2, checksum, (short) 1, (short) 0, (short) 1);
            assertTileCreated(versionId, JSON_2, checksum, (short) 1, (short) 1, (short) 0);
            assertTileCreated(versionId, JSON_2, checksum, (short) 1, (short) 1, (short) 1);

            final TileCompleted tileCompleted0 = new TileCompleted(versionId, (short) 0, (short) 0, (short) 0);

            final TileCompleted tileCompleted1 = new TileCompleted(versionId, (short) 1, (short) 0, (short) 0);
            final TileCompleted tileCompleted2 = new TileCompleted(versionId, (short) 1, (short) 0, (short) 1);
            final TileCompleted tileCompleted3 = new TileCompleted(versionId, (short) 1, (short) 1, (short) 0);
            final TileCompleted tileCompleted4 = new TileCompleted(versionId, (short) 1, (short) 1, (short) 1);

            final Message tileCompletedMessage0 = createTileCompletedMessage(UUID.randomUUID(), versionId, messageTimestamp, tileCompleted0);

            final Message tileCompletedMessage1 = createTileCompletedMessage(UUID.randomUUID(), versionId, messageTimestamp, tileCompleted1);
            final Message tileCompletedMessage2 = createTileCompletedMessage(UUID.randomUUID(), versionId, messageTimestamp, tileCompleted2);
            final Message tileCompletedMessage3 = createTileCompletedMessage(UUID.randomUUID(), versionId, messageTimestamp, tileCompleted3);
            final Message tileCompletedMessage4 = createTileCompletedMessage(UUID.randomUUID(), versionId, messageTimestamp, tileCompleted4);

            safelyClearMessages();

            producer.send(createKafkaRecord(tileCompletedMessage0));
            producer.send(createKafkaRecord(tileCompletedMessage1));
            producer.send(createKafkaRecord(tileCompletedMessage2));
            producer.send(createKafkaRecord(tileCompletedMessage3));
            producer.send(createKafkaRecord(tileCompletedMessage4));

            assertTileCompleted(versionId, (short) 0, (short) 0, (short) 0);

            assertLevelCompleted(versionId, (short) 0);

            assertTileCompleted(versionId, (short) 1, (short) 0, (short) 0);
            assertTileCompleted(versionId, (short) 1, (short) 0, (short) 1);
            assertTileCompleted(versionId, (short) 1, (short) 1, (short) 0);
            assertTileCompleted(versionId, (short) 1, (short) 1, (short) 1);

            assertLevelCompleted(versionId, (short) 1);
        }

        private UUID assertVersionCreated(String checksum, String json) {
            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM VERSION_ENTITY WHERE DESIGN_CHECKSUM = ?")
                                .map(stmt -> stmt.bind(checksum))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(1);
                        rows.forEach(row -> {
                            UUID actualUuid = row.getUuid("VERSION_UUID");
                            Instant actualCreated = row.getInstant("VERSION_CREATED");
                            Instant actualUpdated = row.getInstant("VERSION_UPDATED");
                            Instant actualPublished = row.getInstant("VERSION_PUBLISHED");
                            String actualJson = row.get("DESIGN_DATA", String.class);
                            String actualChecksum = row.get("DESIGN_CHECKSUM", String.class);
                            assertThat(actualUuid).isNotNull();
                            assertThat(actualCreated).isNotNull();
                            assertThat(actualUpdated).isNotNull();
                            assertThat(actualPublished).isNotNull();
                            assertThat(actualJson).isEqualTo(json);
                            assertThat(actualChecksum).isEqualTo(checksum);
                        });
                    });

            final List<Row> versions = session.rxPrepare("SELECT * FROM VERSION_ENTITY WHERE DESIGN_CHECKSUM = ?")
                    .map(stmt -> stmt.bind(checksum))
                    .flatMap(session::rxExecuteWithFullFetch)
                    .toBlocking()
                    .value();

            assertThat(versions).hasSize(1);

            UUID versionId = versions.get(0).getUuid("VERSION_UUID");
            assertThat(versionId).isNotNull();

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(versionId.toString());
                        assertThat(messages).isNotEmpty();
                        final Message actualMessage = messages.stream()
                                .filter(message -> message.getMessageType().equals("design-version-created"))
                                .findFirst()
                                .orElse(null);
                        assertThat(actualMessage).isNotNull();
                        assertThat(actualMessage.getTimestamp()).isNotNull();
                        assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                        assertThat(actualMessage.getPartitionKey()).isEqualTo(versionId.toString());
                        assertThat(actualMessage.getMessageId()).isNotNull();
                        assertThat(actualMessage.getMessageType()).isEqualTo("design-version-created");
                        VersionCreated actualEvent = Json.decodeValue(actualMessage.getMessageBody(), VersionCreated.class);
                        assertThat(actualEvent.getUuid()).isEqualTo(versionId);
                    });

            return versionId;
        }

        private UUID assertLevelCreated(UUID versionId, String json, String checksum, short level) {
            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM RENDER_ENTITY WHERE VERSION_UUID = ? AND RENDER_LEVEL = ?")
                                .map(stmt -> stmt.bind(versionId, level))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(1);
                        rows.forEach(row -> {
                            UUID actualUuid = row.getUuid("RENDER_UUID");
                            Instant actualCreated = row.getInstant("RENDER_CREATED");
                            Instant actualUpdated = row.getInstant("RENDER_UPDATED");
                            Instant actualPublished = row.getInstant("RENDER_PUBLISHED");
                            Instant actualCompleted = row.getInstant("RENDER_COMPLETED");
                            UUID actualVersionUuid = row.getUuid("VERSION_UUID");
                            short actualLevel = row.getShort("RENDER_LEVEL");
                            assertThat(actualUuid).isNotNull();
                            assertThat(actualCreated).isNotNull();
                            assertThat(actualUpdated).isNotNull();
                            assertThat(actualPublished).isNotNull();
                            assertThat(actualCompleted).isNull();
                            assertThat(actualVersionUuid).isEqualTo(versionId);
                            assertThat(actualLevel).isEqualTo(level);
                        });
                    });

            final List<Row> rows = session.rxPrepare("SELECT * FROM RENDER_ENTITY WHERE VERSION_UUID = ? AND RENDER_LEVEL = ?")
                    .map(stmt -> stmt.bind(versionId, level))
                    .flatMap(session::rxExecuteWithFullFetch)
                    .toBlocking()
                    .value();

            assertThat(rows).hasSize(1);

            UUID renderId = rows.get(0).getUuid("RENDER_UUID");
            assertThat(renderId).isNotNull();

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(versionId.toString());
                        assertThat(messages).isNotEmpty();
                        final List<Message> actualMessages = messages.stream()
                                .filter(message -> message.getMessageType().equals("design-render-created"))
                                .collect(Collectors.toList());
                        assertThat(actualMessages).hasSize(3);
                        actualMessages.forEach(actualMessage -> {
                            assertThat(actualMessage.getTimestamp()).isNotNull();
                            assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                            assertThat(actualMessage.getPartitionKey()).isEqualTo(versionId.toString());
                            assertThat(actualMessage.getMessageId()).isNotNull();
                        });
                        RenderCreated actualEvent = actualMessages.stream()
                                .map(message -> Json.decodeValue(message.getMessageBody(), RenderCreated.class))
                                .filter(event -> event.getLevel() == level)
                                .findFirst()
                                .orElse(null);
                        assertThat(actualEvent).isNotNull();
                        assertThat(actualEvent.getUuid()).isEqualTo(versionId);
                        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
                        assertThat(actualEvent.getData()).isEqualTo(json);
                    });

            return renderId;
        }

        private UUID assertLevelCompleted(UUID versionId, short level) {
            await().atMost(ONE_MINUTE)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM RENDER_ENTITY WHERE VERSION_UUID = ? AND RENDER_LEVEL = ?")
                                .map(stmt -> stmt.bind(versionId, level))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(1);
                        rows.forEach(row -> {
                            UUID actualUuid = row.getUuid("RENDER_UUID");
                            Instant actualCreated = row.getInstant("RENDER_CREATED");
                            Instant actualUpdated = row.getInstant("RENDER_UPDATED");
                            Instant actualPublished = row.getInstant("RENDER_PUBLISHED");
                            Instant actualCompleted = row.getInstant("RENDER_COMPLETED");
                            UUID actualVersionUuid = row.getUuid("VERSION_UUID");
                            short actualLevel = row.getShort("RENDER_LEVEL");
                            assertThat(actualUuid).isNotNull();
                            assertThat(actualCreated).isNotNull();
                            assertThat(actualUpdated).isNotNull();
                            assertThat(actualPublished).isNotNull();
                            assertThat(actualCompleted).isNotNull();
                            assertThat(actualVersionUuid).isEqualTo(versionId);
                            assertThat(actualLevel).isEqualTo(level);
                        });
                    });

            final List<Row> rows = session.rxPrepare("SELECT * FROM RENDER_ENTITY WHERE VERSION_UUID = ? AND RENDER_LEVEL = ?")
                    .map(stmt -> stmt.bind(versionId, level))
                    .flatMap(session::rxExecuteWithFullFetch)
                    .toBlocking()
                    .value();

            assertThat(rows).hasSize(1);

            UUID renderId = rows.get(0).getUuid("RENDER_UUID");
            assertThat(renderId).isNotNull();

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(versionId.toString());
                        assertThat(messages).isNotEmpty();
                        final List<Message> actualMessages = messages.stream()
                                .filter(message -> message.getMessageType().equals("design-render-completed"))
                                .collect(Collectors.toList());
                        assertThat(actualMessages).hasSize(2);
                        actualMessages.forEach(actualMessage -> {
                            assertThat(actualMessage.getTimestamp()).isNotNull();
                            assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                            assertThat(actualMessage.getPartitionKey()).isEqualTo(versionId.toString());
                            assertThat(actualMessage.getMessageId()).isNotNull();
                        });
                        RenderCompleted actualEvent = actualMessages.stream()
                                .map(message -> Json.decodeValue(message.getMessageBody(), RenderCompleted.class))
                                .filter(event -> event.getLevel() == level)
                                .findFirst()
                                .orElse(null);
                        assertThat(actualEvent).isNotNull();
                        assertThat(actualEvent.getUuid()).isEqualTo(versionId);
                    });

            return renderId;
        }

        private UUID assertTileCreated(UUID versionId, String json, String checksum, short level, short x, short y) {
            await().atMost(Duration.of(30L, ChronoUnit.SECONDS))
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM TILE_ENTITY WHERE VERSION_UUID = ? AND TILE_LEVEL = ? AND TILE_ROW = ? AND TILE_COL = ?")
                                .map(stmt -> stmt.bind(versionId, level, y, x))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(1);
                        rows.forEach(row -> {
                            UUID actualUuid = row.getUuid("TILE_UUID");
                            Instant actualCreated = row.getInstant("TILE_CREATED");
                            Instant actualUpdated = row.getInstant("TILE_UPDATED");
                            Instant actualPublished = row.getInstant("TILE_PUBLISHED");
                            Instant actualCompleted = row.getInstant("TILE_COMPLETED");
                            UUID actualVersionUuid = row.getUuid("VERSION_UUID");
                            short actualLevel = row.getShort("TILE_LEVEL");
                            assertThat(actualUuid).isNotNull();
                            assertThat(actualCreated).isNotNull();
                            assertThat(actualUpdated).isNotNull();
                            assertThat(actualPublished).isNotNull();
                            assertThat(actualCompleted).isNull();
                            assertThat(actualVersionUuid).isEqualTo(versionId);
                            assertThat(actualLevel).isEqualTo(level);
                        });
                    });

            final List<Row> rows = session.rxPrepare("SELECT * FROM TILE_ENTITY WHERE VERSION_UUID = ? AND TILE_LEVEL = ? AND TILE_ROW = ? AND TILE_COL = ?")
                    .map(stmt -> stmt.bind(versionId, level, y, x))
                    .flatMap(session::rxExecuteWithFullFetch)
                    .toBlocking()
                    .value();

            assertThat(rows).hasSize(1);

            UUID tileId = rows.get(0).getUuid("TILE_UUID");
            assertThat(tileId).isNotNull();

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(versionId.toString());
                        assertThat(messages).isNotEmpty();
                        final List<Message> actualMessages = messages.stream()
                                .filter(message -> message.getMessageType().equals("design-tile-created"))
                                .collect(Collectors.toList());
                        assertThat(actualMessages).hasSize(21);
                        actualMessages.forEach(actualMessage -> {
                            assertThat(actualMessage.getTimestamp()).isNotNull();
                            assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                            assertThat(actualMessage.getPartitionKey()).isEqualTo(versionId.toString());
                            assertThat(actualMessage.getMessageId()).isNotNull();
                        });
                        TileCreated actualEvent = actualMessages.stream()
                                .map(message -> Json.decodeValue(message.getMessageBody(), TileCreated.class))
                                .filter(event -> event.getLevel() == level)
                                .filter(event -> event.getX() == x)
                                .filter(event -> event.getY() == y)
                                .findFirst()
                                .orElse(null);
                        assertThat(actualEvent).isNotNull();
                        assertThat(actualEvent.getUuid()).isEqualTo(versionId);
                        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
                        assertThat(actualEvent.getData()).isEqualTo(json);
                    });

            return tileId;
        }

        private UUID assertTileCompleted(UUID versionId, short level, short x, short y) {
            await().atMost(ONE_MINUTE)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Row> rows = session.rxPrepare("SELECT * FROM TILE_ENTITY WHERE VERSION_UUID = ? AND TILE_LEVEL = ? AND TILE_ROW = ? AND TILE_COL = ?")
                                .map(stmt -> stmt.bind(versionId, level, y, x))
                                .flatMap(session::rxExecuteWithFullFetch)
                                .toBlocking()
                                .value();
                        assertThat(rows).hasSize(1);
                        rows.forEach(row -> {
                            UUID actualUuid = row.getUuid("TILE_UUID");
                            Instant actualCreated = row.getInstant("TILE_CREATED");
                            Instant actualUpdated = row.getInstant("TILE_UPDATED");
                            Instant actualPublished = row.getInstant("TILE_PUBLISHED");
                            Instant actualCompleted = row.getInstant("TILE_COMPLETED");
                            UUID actualVersionUuid = row.getUuid("VERSION_UUID");
                            short actualLevel = row.getShort("TILE_LEVEL");
                            assertThat(actualUuid).isNotNull();
                            assertThat(actualCreated).isNotNull();
                            assertThat(actualUpdated).isNotNull();
                            assertThat(actualPublished).isNotNull();
                            assertThat(actualCompleted).isNotNull();
                            assertThat(actualVersionUuid).isEqualTo(versionId);
                            assertThat(actualLevel).isEqualTo(level);
                        });
                    });

            final List<Row> rows = session.rxPrepare("SELECT * FROM TILE_ENTITY WHERE VERSION_UUID = ? AND TILE_LEVEL = ? AND TILE_ROW = ? AND TILE_COL = ?")
                    .map(stmt -> stmt.bind(versionId, level, y, x))
                    .flatMap(session::rxExecuteWithFullFetch)
                    .toBlocking()
                    .value();

            assertThat(rows).hasSize(1);

            UUID tileId = rows.get(0).getUuid("TILE_UUID");
            assertThat(tileId).isNotNull();

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(versionId.toString());
                        assertThat(messages).isNotEmpty();
                        final List<Message> actualMessages = messages.stream()
                                .filter(message -> message.getMessageType().equals("design-tile-completed"))
                                .collect(Collectors.toList());
                        assertThat(actualMessages).hasSize(5);
                        actualMessages.forEach(actualMessage -> {
                            assertThat(actualMessage.getTimestamp()).isNotNull();
                            assertThat(actualMessage.getMessageSource()).isEqualTo("test");
                            assertThat(actualMessage.getPartitionKey()).isEqualTo(versionId.toString());
                            assertThat(actualMessage.getMessageId()).isNotNull();
                        });
                        TileCompleted actualEvent = actualMessages.stream()
                                .map(message -> Json.decodeValue(message.getMessageBody(), TileCompleted.class))
                                .filter(event -> event.getLevel() == level)
                                .filter(event -> event.getX() == x)
                                .filter(event -> event.getY() == y)
                                .findFirst()
                                .orElseThrow();
                        assertThat(actualEvent.getUuid()).isEqualTo(versionId);
                    });

            return tileId;
        }
    }

    private static ProducerRecord<String, String> createKafkaRecord(Message message) {
        return new ProducerRecord<>("design-event", message.getPartitionKey(), Json.encode(message));
    }

    private static Message createDesignChangedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChanged event) {
        return new Message(messageId.toString(), MessageType.DESIGN_CHANGED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    private static Message createTileCompletedMessage(UUID messageId, UUID partitionKey, long timestamp, TileCompleted event) {
        return new Message(messageId.toString(), MessageType.DESIGN_TILE_COMPLETED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    private static List<Message> safelyFindMessages(String versionUuid) {
        synchronized (records) {
            return records.stream()
                    .map(record -> Json.decodeValue(record.value(), Message.class))
                    .filter(value -> value.getPartitionKey().equals(versionUuid))
                    .collect(Collectors.toList());
        }
    }

    private static void safelyClearMessages() {
        synchronized (records) {
            records.clear();
        }
    }

    private static void safelyAppendRecord(ConsumerRecord<String, String> record) {
        synchronized (records) {
            records.add(record);
        }
    }

    private static Thread createConsumerThread() {
        return new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(5));
                    System.out.println("Received " + consumerRecords.count() + " messages");
                    consumerRecords.forEach(IntegrationTests::safelyAppendRecord);
                    consumer.commitSync();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
