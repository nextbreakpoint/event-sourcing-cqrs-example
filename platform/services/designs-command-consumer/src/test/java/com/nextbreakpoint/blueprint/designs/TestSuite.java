package com.nextbreakpoint.blueprint.designs;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;
import com.jayway.restassured.RestAssured;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.command.DeleteDesign;
import com.nextbreakpoint.blueprint.common.core.command.InsertDesign;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClusterFactory;
import io.vertx.core.json.Json;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;

@Tag("slow")
public class TestSuite {
    private static final String JSON_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    private static final String JSON_2 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 100] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";

    private static final TestScenario scenario = new TestScenario();

    private static Environment environment = Environment.getDefaultEnvironment();

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        scenario.after();
    }

    @Nested
    @Tag("integration")
    @DisplayName("Verify behaviour of designs-command-consumer service")
    public class VerifyServiceIntegration {
        @AfterEach
        public void reset() {
            RestAssured.reset();
        }

        @Test
        @DisplayName("Should insert a design after receiving a DesignInsert event")
        public void shouldInsertDesignWhenReceivingAMessage() throws InterruptedException {
            final KafkaConsumer<String, String>[] consumer = new KafkaConsumer[1];

            KafkaProducer<String, String> producer = null;

            Thread polling = null;

            try {
                final Cluster cluster = CassandraClusterFactory.create(environment, scenario.createCassandraConfig());

                producer = KafkaUtils.createProducer(environment, scenario.createProducerConfig());

                consumer[0] = KafkaUtils.createConsumer(environment, scenario.createConsumerConfig("test-insert"));

                consumer[0].subscribe(Collections.singleton("designs-sse"));

                final UUID eventTimestamp1 = UUIDs.timeBased();

                final UUID designId = UUID.randomUUID();

                final InsertDesign insertDesignCommand = new InsertDesign(designId, JSON_1, eventTimestamp1);

                final long messageTimestamp = System.currentTimeMillis();

                final Message insertDesignMessage = createInsertDesignMessage(UUID.randomUUID(), designId, messageTimestamp, insertDesignCommand);

                final List<ConsumerRecord<String, String>> records = new ArrayList<>();

                polling = createConsumerThread(records, consumer[0]);

                polling.start();

                producer.send(createKafkaRecord(insertDesignMessage));

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            try (Session session = cluster.connect("designs")) {
                                final PreparedStatement statement = session.prepare("SELECT * FROM DESIGNS WHERE DESIGN_UUID = ?");
                                final List<Row> rows = session.execute(statement.bind(designId)).all();
                                assertThat(rows).hasSize(1);
                                rows.forEach(row -> {
                                    String actualJson = row.get("DESIGN_JSON", String.class);
                                    String actualStatus = row.get("DESIGN_STATUS", String.class);
                                    String actualChecksum = row.get("DESIGN_CHECKSUM", String.class);
                                    assertThat(actualJson).isEqualTo(JSON_1);
                                    assertThat(actualStatus).isEqualTo("CREATED");
                                    assertThat(actualChecksum).isNotNull();
                                });
                            }
                        });

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            try (Session session = cluster.connect("designs")) {
                                final PreparedStatement statement = session.prepare("SELECT * FROM DESIGNS_VIEW WHERE DESIGN_UUID = ?");
                                final List<Row> rows = session.execute(statement.bind(designId)).all();
                                assertThat(rows).hasSize(1);
                                rows.forEach(row -> {
                                    String actualJson = row.get("DESIGN_JSON", String.class);
                                    String actualChecksum = row.get("DESIGN_CHECKSUM", String.class);
                                    assertThat(actualJson).isEqualTo(JSON_1);
                                    assertThat(actualChecksum).isNotNull();
                                });
                            }
                        });

                await().atMost(TEN_MINUTES)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            final List<Message> messages = safelyFindMessages(records, designId);
                            assertThat(messages).hasSize(1);
                            final Message actualMessage = messages.get(messages.size() - 1);
                            assertThat(actualMessage.getTimestamp()).isNotNull();
                            assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                            assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
                            assertThat(actualMessage.getMessageId()).isNotNull();
                            assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                            DesignChanged actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChanged.class);
                            assertThat(actualEvent.getUuid()).isEqualTo(designId);
                            assertThat(actualEvent.getTimestamp()).isNotNull();
                            assertThat(actualEvent.getTimestamp()).isGreaterThan(UUIDs.unixTimestamp(eventTimestamp1));
                        });
            } finally {
                if (polling != null) {
                    polling.interrupt();
                    polling.join();
                }
                if (consumer[0] != null) {
                    consumer[0].close();
                }
                if (producer != null) {
                    producer.close();
                }
            }
        }

        @Test
        @DisplayName("Should update a design after receiving a DesignUpdate event")
        public void shouldUpdateDesignWhenReceivingAMessage() throws InterruptedException {
            final KafkaConsumer<String, String>[] consumer = new KafkaConsumer[1];

            KafkaProducer<String, String> producer = null;

            Thread polling = null;

            try {
                final Cluster cluster = CassandraClusterFactory.create(environment, scenario.createCassandraConfig());

                producer = KafkaUtils.createProducer(environment, scenario.createProducerConfig());

                consumer[0] = KafkaUtils.createConsumer(environment, scenario.createConsumerConfig("test-update"));

                consumer[0].subscribe(Collections.singleton("designs-sse"));

                final UUID eventTimestamp1 = UUIDs.timeBased();

                final UUID eventTimestamp2 = UUIDs.timeBased();

                final UUID designId = UUID.randomUUID();

                final InsertDesign insertDesignCommand = new InsertDesign(designId, JSON_1, eventTimestamp1);

                final UpdateDesign updateDesignCommand = new UpdateDesign(designId, JSON_2, eventTimestamp2);

                final long messageTimestamp = System.currentTimeMillis();

                final Message insertDesignMessage = createInsertDesignMessage(UUID.randomUUID(), designId, messageTimestamp, insertDesignCommand);

                final Message updateDesignMessage = createUpdateDesignMessage(UUID.randomUUID(), designId, messageTimestamp, updateDesignCommand);

                final List<ConsumerRecord<String, String>> records = new ArrayList<>();

                polling = createConsumerThread(records, consumer[0]);

                polling.start();

                producer.send(createKafkaRecord(insertDesignMessage));

                producer.send(createKafkaRecord(updateDesignMessage));

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            try (Session session = cluster.connect("designs")) {
                                final PreparedStatement statement = session.prepare("SELECT * FROM DESIGNS WHERE DESIGN_UUID = ?");
                                final List<Row> rows = session.execute(statement.bind(designId)).all();
                                assertThat(rows).hasSize(2);
                                final Set<UUID> uuids = rows.stream()
                                        .map(row -> row.getUUID("DESIGN_UUID"))
                                        .collect(Collectors.toSet());
                                assertThat(uuids).contains(designId);
                                String actualJson1 = rows.get(0).get("DESIGN_JSON", String.class);
                                String actualStatus1 = rows.get(0).get("DESIGN_STATUS", String.class);
                                assertThat(actualJson1).isEqualTo(JSON_1);
                                assertThat(actualStatus1).isEqualTo("CREATED");
                                String actualJson2 = rows.get(1).get("DESIGN_JSON", String.class);
                                String actualStatus2 = rows.get(1).get("DESIGN_STATUS", String.class);
                                assertThat(actualJson2).isEqualTo(JSON_2);
                                assertThat(actualStatus2).isEqualTo("UPDATED");
                            }
                        });

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            try (Session session = cluster.connect("designs")) {
                                final PreparedStatement statement = session.prepare("SELECT * FROM DESIGNS_VIEW WHERE DESIGN_UUID = ?");
                                final List<Row> rows = session.execute(statement.bind(designId)).all();
                                assertThat(rows).hasSize(1);
                                rows.forEach(row -> {
                                    String actualJson = row.get("DESIGN_JSON", String.class);
                                    String actualChecksum = row.get("DESIGN_CHECKSUM", String.class);
                                    assertThat(actualJson).isEqualTo(JSON_2);
                                    assertThat(actualChecksum).isNotNull();
                                });
                            }
                        });

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            final List<Message> messages = safelyFindMessages(records, designId);
                            assertThat(messages).hasSize(2);
                            final Message actualMessage = messages.get(messages.size() - 1);
                            assertThat(actualMessage.getTimestamp()).isNotNull();
                            assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                            assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
                            assertThat(actualMessage.getMessageId()).isNotNull();
                            assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                            DesignChanged actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChanged.class);
                            assertThat(actualEvent.getUuid()).isEqualTo(designId);
                            assertThat(actualEvent.getTimestamp()).isNotNull();
                            assertThat(actualEvent.getTimestamp()).isGreaterThan(UUIDs.unixTimestamp(eventTimestamp1));
                        });
            } finally {
                if (polling != null) {
                    polling.interrupt();
                    polling.join();
                }
                if (consumer[0] != null) {
                    consumer[0].close();
                }
                if (producer != null) {
                    producer.close();
                }
            }
        }

        @Test
        @DisplayName("Should delete a design after receiving a DesignDelete event")
        public void shouldDeleteDesignWhenReceivingAMessage() throws InterruptedException {
            final KafkaConsumer<String, String>[] consumer = new KafkaConsumer[1];

            KafkaProducer<String, String> producer = null;

            Thread polling = null;

            try {
                final Cluster cluster = CassandraClusterFactory.create(environment, scenario.createCassandraConfig());

                producer = KafkaUtils.createProducer(environment, scenario.createProducerConfig());

                consumer[0] = KafkaUtils.createConsumer(environment, scenario.createConsumerConfig("test-delete"));

                consumer[0].subscribe(Collections.singleton("designs-sse"));

                final UUID eventTimestamp1 = UUIDs.timeBased();

                final UUID eventTimestamp2 = UUIDs.timeBased();

                final UUID designId = UUID.randomUUID();

                final InsertDesign insertDesignCommand = new InsertDesign(designId, JSON_1, eventTimestamp1);

                final DeleteDesign deleteDesignCommand = new DeleteDesign(designId, eventTimestamp2);

                final long messageTimestamp = System.currentTimeMillis();

                final Message insertDesignMessage = createInsertDesignMessage(UUID.randomUUID(), designId, messageTimestamp, insertDesignCommand);

                final Message deleteDesignMessage = createDeleteDesignMessage(UUID.randomUUID(), designId, messageTimestamp, deleteDesignCommand);

                final List<ConsumerRecord<String, String>> records = new ArrayList<>();

                polling = createConsumerThread(records, consumer[0]);

                polling.start();

                producer.send(createKafkaRecord(insertDesignMessage));

                producer.send(createKafkaRecord(deleteDesignMessage));

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            try (Session session = cluster.connect("designs")) {
                                final PreparedStatement statement = session.prepare("SELECT * FROM DESIGNS WHERE DESIGN_UUID = ?");
                                final List<Row> rows = session.execute(statement.bind(designId)).all();
                                assertThat(rows).hasSize(2);
                                final Set<UUID> uuids = rows.stream()
                                        .map(row -> row.getUUID("DESIGN_UUID"))
                                        .collect(Collectors.toSet());
                                assertThat(uuids).contains(designId);
                                String actualJson1 = rows.get(0).get("DESIGN_JSON", String.class);
                                String actualStatus1 = rows.get(0).get("DESIGN_STATUS", String.class);
                                assertThat(actualJson1).isEqualTo(JSON_1);
                                assertThat(actualStatus1).isEqualTo("CREATED");
                                String actualJson2 = rows.get(1).get("DESIGN_JSON", String.class);
                                String actualStatus2 = rows.get(1).get("DESIGN_STATUS", String.class);
                                assertThat(actualJson2).isNull();
                                assertThat(actualStatus2).isEqualTo("DELETED");
                            }
                        });

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            try (Session session = cluster.connect("designs")) {
                                final PreparedStatement statement = session.prepare("SELECT * FROM DESIGNS_VIEW WHERE DESIGN_UUID = ?");
                                final List<Row> rows = session.execute(statement.bind(designId)).all();
                                assertThat(rows).hasSize(0);
                            }
                        });

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            final List<Message> messages = safelyFindMessages(records, designId);
                            assertThat(messages).hasSize(2);
                            final Message actualMessage = messages.get(messages.size() - 1);
                            assertThat(actualMessage.getTimestamp()).isNotNull();
                            assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                            assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
                            assertThat(actualMessage.getMessageId()).isNotNull();
                            assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                            DesignChanged actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChanged.class);
                            assertThat(actualEvent.getUuid()).isEqualTo(designId);
                            assertThat(actualEvent.getTimestamp()).isNotNull();
                            assertThat(actualEvent.getTimestamp()).isGreaterThan(UUIDs.unixTimestamp(eventTimestamp1));
                        });
            } finally {
                if (polling != null) {
                    polling.interrupt();
                    polling.join();
                }
                if (consumer[0] != null) {
                    consumer[0].close();
                }
                if (producer != null) {
                    producer.close();
                }
            }
        }

        private List<Message> safelyFindMessages(List<ConsumerRecord<String, String>> records, UUID designId) {
            synchronized (records) {
                return records.stream()
                        .map(record -> Json.decodeValue(record.value(), Message.class))
                        .filter(value -> value.getPartitionKey().equals(designId.toString()))
                        .collect(Collectors.toList());
            }
        }

        private void safelyAppendRecord(List<ConsumerRecord<String, String>> records, ConsumerRecord<String, String> record) {
            synchronized (records) {
                records.add(record);
            }
        }

        private Thread createConsumerThread(List<ConsumerRecord<String, String>> records, KafkaConsumer<String, String> kafkaConsumer) {
            return new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(5));
                        System.out.println("Received " + consumerRecords.count() + " messages");
                        consumerRecords.forEach(consumerRecord -> safelyAppendRecord(records, consumerRecord));
                        kafkaConsumer.commitSync();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        private ProducerRecord<String, String> createKafkaRecord(Message message) {
            return new ProducerRecord<>("designs-events", message.getPartitionKey(), Json.encode(message));
        }

        private Message createInsertDesignMessage(UUID messageId, UUID partitionKey, long timestamp, InsertDesign event) {
            return new Message(messageId.toString(), MessageType.DESIGN_INSERT, Json.encode(event), "test", partitionKey.toString(), timestamp);
        }

        private Message createUpdateDesignMessage(UUID messageId, UUID partitionKey, long timestamp, UpdateDesign event) {
            return new Message(messageId.toString(), MessageType.DESIGN_UPDATE, Json.encode(event), "test", partitionKey.toString(), timestamp);
        }

        private Message createDeleteDesignMessage(UUID messageId, UUID partitionKey, long timestamp, DeleteDesign event) {
            return new Message(messageId.toString(), MessageType.DESIGN_DELETE, Json.encode(event), "test", partitionKey.toString(), timestamp);
        }
    }
}
