package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.jayway.restassured.RestAssured;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import com.nextbreakpoint.blueprint.designs.model.TileCompleted;
import com.nextbreakpoint.blueprint.designs.model.TileCreated;
import io.vertx.core.json.Json;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import rx.Single;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;

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

        final S3Client s3Client = createS3Client();

        s3Client.createBucket(CreateBucketRequest.builder().bucket("tiles").build());
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
    @DisplayName("Verify behaviour of designs-command-consumer service")
    public class VerifyServiceApi {
        @AfterEach
        public void reset() {
            RestAssured.reset();
        }

        @Test
        @DisplayName("Should render the tile's image after receiving a tile created event")
        public void shouldRenderImageWhenReceivingAMessage() {
            session.rxPrepare("TRUNCATE TILE_ENTITY")
                    .map(PreparedStatement::bind)
                    .flatMap(session::rxExecute)
                    .toBlocking()
                    .value();

            final UUID versionId = UUID.randomUUID();

            final String checksum = UUID.randomUUID().toString();

            final short level = (short) 0;

            final short x = (short) 0;
            final short y = (short) 0;

            final Single<PreparedStatement> preparedStatementSingle = session.rxPrepare("INSERT INTO TILE_ENTITY (TILE_UUID, VERSION_UUID, TILE_LEVEL, TILE_ROW, TILE_COL, TILE_CREATED, TILE_UPDATED, TILE_PUBLISHED) VALUES (?,?,?,?,?,toTimeStamp(now()),toTimeStamp(now()),toTimeStamp(now()))");

            final UUID tileId = UUID.randomUUID();

            preparedStatementSingle
                    .map(stmt -> stmt.bind(tileId, versionId, level, y, x))
                    .flatMap(session::rxExecute)
                    .toBlocking()
                    .value();

            final TileCreated tileCreated = new TileCreated(versionId, JSON_1, checksum, level, x, y);

            final long messageTimestamp = System.currentTimeMillis();

            final Message tileCreatedMessage = createTileCreatedMessage(UUID.randomUUID(), versionId, messageTimestamp, tileCreated);

            safelyClearMessages();

            producer.send(createKafkaRecord(tileCreatedMessage));

            UUID actualTileId = assertTileCompleted(versionId, level, x ,y);

            assertThat(actualTileId).isEqualTo(tileId);
        }

        @NotNull
        private UUID assertTileCompleted(UUID versionId, short level, short x, short y) {
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
                            UUID actualUuid = row.getUuid("VERSION_UUID");
                            Instant actualCreated = row.getInstant("TILE_CREATED");
                            Instant actualUpdated = row.getInstant("TILE_UPDATED");
                            Instant actualPublished = row.getInstant("TILE_PUBLISHED");
                            Instant actualCompleted = row.getInstant("TILE_COMPLETED");
                            assertThat(actualUuid).isNotNull();
                            assertThat(actualCreated).isNotNull();
                            assertThat(actualUpdated).isNotNull();
                            assertThat(actualPublished).isNotNull();
                            assertThat(actualCompleted).isNotNull();
                        });
                    });

            final List<Row> versions = session.rxPrepare("SELECT * FROM TILE_ENTITY WHERE VERSION_UUID = ? AND TILE_LEVEL = ? AND TILE_ROW = ? AND TILE_COL = ?")
                    .map(stmt -> stmt.bind(versionId, level, y, x))
                    .flatMap(session::rxExecuteWithFullFetch)
                    .toBlocking()
                    .value();

            assertThat(versions).hasSize(1);

            UUID tileId = versions.get(0).getUuid("TILE_UUID");
            assertThat(tileId).isNotNull();

            await().atMost(Duration.of(30L, ChronoUnit.SECONDS))
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(versionId.toString());
                        assertThat(messages).isNotEmpty();
                        final Message actualMessage = messages.stream()
                                .filter(message -> message.getMessageType().equals("design-tile-completed"))
                                .findFirst()
                                .orElseThrow();
                        assertThat(actualMessage.getTimestamp()).isNotNull();
                        assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                        assertThat(actualMessage.getPartitionKey()).isEqualTo(versionId.toString());
                        assertThat(actualMessage.getMessageId()).isNotNull();
                        assertThat(actualMessage.getMessageType()).isEqualTo("design-tile-completed");
                        TileCompleted actualEvent = Json.decodeValue(actualMessage.getMessageBody(), TileCompleted.class);
                        assertThat(actualEvent.getUuid()).isEqualTo(versionId);
                        assertThat(actualEvent.getLevel()).isEqualTo(level);
                        assertThat(actualEvent.getX()).isEqualTo(x);
                        assertThat(actualEvent.getY()).isEqualTo(y);
                    });

            return tileId;
        }
    }

    private static ProducerRecord<String, String> createKafkaRecord(Message message) {
        return new ProducerRecord<>("design-event", message.getPartitionKey(), Json.encode(message));
    }

    private static Message createTileCreatedMessage(UUID messageId, UUID partitionKey, long timestamp, TileCreated event) {
        return new Message(messageId.toString(), MessageType.DESIGN_TILE_CREATED, Json.encode(event), "test", partitionKey.toString(), timestamp);
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

    private static S3Client createS3Client() {
        return S3Client.builder()
                .region(Region.EU_WEST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("admin", "password")))
                .endpointOverride(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()))
                .build();
    }
}
