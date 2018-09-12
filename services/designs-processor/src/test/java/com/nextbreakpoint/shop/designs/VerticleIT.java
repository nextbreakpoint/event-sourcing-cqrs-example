package com.nextbreakpoint.shop.designs;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.nextbreakpoint.shop.common.cassandra.CassandraClusterFactory;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.UpdateDesignCommand;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.common.vertx.KafkaClientFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.TEN_SECONDS;
import static org.awaitility.Duration.TWO_SECONDS;

@Tag("slow")
@DisplayName("Verify contract of service Designs Processor")
public class VerticleIT {
    private static final String JSON_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    private static final String JSON_2 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 100] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";

    private static RestAssuredConfig restAssuredConfig;

    private Vertx vertx;

    @BeforeAll
    public static void configureRestAssured() {
        final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
        final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
        final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        restAssuredConfig = RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
    }

    @AfterAll
    public static void unconfigureRestAssured() {
        RestAssured.reset();
    }

    @BeforeEach
    public void createVertx() {
        vertx = new Vertx(io.vertx.core.Vertx.vertx());
    }

    @AfterEach
    public void destroyVertx() {
        vertx.close();
    }

    @Test
    @DisplayName("Should insert a design after receiving a DesignInsert event")
    public void shouldInsertDesignWhenRecevingAMessage() throws IOException {
        KafkaConsumer<String, String> consumer = null;

        KafkaProducer<String, String> producer = null;

        try {
            final Cluster cluster = CassandraClusterFactory.create(createCassandraConfig());

            producer = KafkaClientFactory.createProducer(vertx, createProducerConfig());

            consumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig("insert-design"));

            String[] message = new String[]{null};

            consumer.handler(record -> message[0] = record.value())
                    .rxSubscribe("designs-sse")
                    .subscribe();

            long eventTimestamp = System.currentTimeMillis();

            final UUID designId = UUID.randomUUID();

            final InsertDesignCommand insertDesignCommand = new InsertDesignCommand(designId, JSON_1, eventTimestamp);

            final long messageTimestamp = System.currentTimeMillis();

            final Message insertDesignMessage = createInsertDesignMessage(UUID.randomUUID(), designId, messageTimestamp, insertDesignCommand);

            producer.rxWrite(createKafkaRecord(insertDesignMessage)).subscribe();

            await().atMost(FIVE_SECONDS)
                    .pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        try (Session session = cluster.connect("designs")) {
                            final PreparedStatement statement = session.prepare("SELECT * FROM DESIGNS WHERE UUID = ?");
                            final List<Row> rows = session.execute(statement.bind(designId)).all();
                            assertThat(rows).hasSize(1);
                            rows.forEach(row -> {
                                String actualJson = row.get("JSON", String.class);
                                String actualStatus = row.get("STATUS", String.class);
                                String actualChecksum = row.get("CHECKSUM", String.class);
                                assertThat(actualJson).isEqualTo(JSON_1);
                                assertThat(actualStatus).isEqualTo("CREATED");
                                assertThat(actualChecksum).isNotNull();
                            });
                        }
                    });

            await().atMost(FIVE_SECONDS)
                    .pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(message[0]).isNotNull();
                        Message actualMessage = Json.decodeValue(message[0], Message.class);
                        assertThat(actualMessage.getTimestamp()).isNotNull();
                        assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
                        assertThat(actualMessage.getMessageId()).isNotNull();
                        assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                        DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChangedEvent.class);
                        assertThat(actualEvent.getUuid()).isEqualTo(designId);
                        assertThat(actualEvent.getTimestamp()).isNotNull();
                        assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp);
                    });
        } finally {
            if (consumer != null) {
                consumer.close();
            }
            if (producer != null) {
                producer.close();
            }
        }
    }

    @Test
    @DisplayName("Should update a design after receiving a DesignUpdate event")
    public void shouldUpdateDesignWhenRecevingAMessage() throws IOException {
        KafkaConsumer<String, String> consumer = null;

        KafkaProducer<String, String> producer = null;

        try {
            final Cluster cluster = CassandraClusterFactory.create(createCassandraConfig());

            producer = KafkaClientFactory.createProducer(vertx, createProducerConfig());

            consumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig("update-design"));

            String[] message = new String[]{null};

            consumer.handler(record -> message[0] = record.value())
                    .rxSubscribe("designs-sse")
                    .subscribe();

            long eventTimestamp = System.currentTimeMillis();

            final UUID designId = UUID.randomUUID();

            final InsertDesignCommand insertDesignCommand = new InsertDesignCommand(designId, JSON_1, eventTimestamp - 2);

            final UpdateDesignCommand updateDesignCommand = new UpdateDesignCommand(designId, JSON_2, eventTimestamp - 1);

            final long messageTimestamp = System.currentTimeMillis();

            final Message insertDesignMessage = createInsertDesignMessage(UUID.randomUUID(), designId, messageTimestamp, insertDesignCommand);

            final Message updateDesignMessage = createUpdateDesignMessage(UUID.randomUUID(), designId, messageTimestamp, updateDesignCommand);

            producer.rxWrite(createKafkaRecord(insertDesignMessage)).subscribe();

            producer.rxWrite(createKafkaRecord(updateDesignMessage)).subscribe();

            await().atMost(FIVE_SECONDS)
                    .pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        try (Session session = cluster.connect("designs")) {
                            final PreparedStatement statement = session.prepare("SELECT * FROM DESIGNS WHERE UUID = ?");
                            final List<Row> rows = session.execute(statement.bind(designId)).all();
                            assertThat(rows).hasSize(2);
                            final Set<UUID> uuids = rows.stream()
                                    .map(row -> row.getUUID("UUID"))
                                    .collect(Collectors.toSet());
                            assertThat(uuids).contains(designId);
                            String actualJson1 = rows.get(0).get("JSON", String.class);
                            String actualStatus1 = rows.get(0).get("STATUS", String.class);
                            assertThat(actualJson1).isEqualTo(JSON_1);
                            assertThat(actualStatus1).isEqualTo("CREATED");
                            String actualJson2 = rows.get(1).get("JSON", String.class);
                            String actualStatus2 = rows.get(1).get("STATUS", String.class);
                            assertThat(actualJson2).isEqualTo(JSON_2);
                            assertThat(actualStatus2).isEqualTo("UPDATED");
                        }
                    });

            await().atMost(FIVE_SECONDS)
                    .pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(message[0]).isNotNull();
                        Message actualMessage = Json.decodeValue(message[0], Message.class);
                        assertThat(actualMessage.getTimestamp()).isNotNull();
                        assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
                        assertThat(actualMessage.getMessageId()).isNotNull();
                        assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                        DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChangedEvent.class);
                        assertThat(actualEvent.getUuid()).isEqualTo(designId);
                        assertThat(actualEvent.getTimestamp()).isNotNull();
                        assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp);
                    });
        } finally {
            if (consumer != null) {
                consumer.close();
            }
            if (producer != null) {
                producer.close();
            }
        }
    }

    @Test
    @DisplayName("Should delete a design after receiving a DesignDelete event")
    public void shouldDeleteDesignWhenRecevingAMessage() throws IOException {
        KafkaConsumer<String, String> consumer = null;

        KafkaProducer<String, String> producer = null;

        try {
            final Cluster cluster = CassandraClusterFactory.create(createCassandraConfig());

            producer = KafkaClientFactory.createProducer(vertx, createProducerConfig());

            consumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig("delete-design"));

            String[] message = new String[]{null};

            consumer.handler(record -> message[0] = record.value())
                    .rxSubscribe("designs-sse")
                    .subscribe();

            long eventTimestamp = System.currentTimeMillis();

            final UUID designId = UUID.randomUUID();

            final InsertDesignCommand insertDesignCommand = new InsertDesignCommand(designId, JSON_1, eventTimestamp - 2);

            final DeleteDesignCommand deleteDesignCommand = new DeleteDesignCommand(designId, eventTimestamp - 1);

            final long messageTimestamp = System.currentTimeMillis();

            final Message insertDesignMessage = createInsertDesignMessage(UUID.randomUUID(), designId, messageTimestamp, insertDesignCommand);

            final Message deleteDesignMessage = createDeleteDesignMessage(UUID.randomUUID(), designId, messageTimestamp, deleteDesignCommand);

            producer.rxWrite(createKafkaRecord(insertDesignMessage)).subscribe();

            producer.rxWrite(createKafkaRecord(deleteDesignMessage)).subscribe();

            await().atMost(FIVE_SECONDS)
                    .pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        try (Session session = cluster.connect("designs")) {
                            final PreparedStatement statement = session.prepare("SELECT * FROM DESIGNS WHERE UUID = ?");
                            final List<Row> rows = session.execute(statement.bind(designId)).all();
                            assertThat(rows).hasSize(2);
                            final Set<UUID> uuids = rows.stream()
                                    .map(row -> row.getUUID("UUID"))
                                    .collect(Collectors.toSet());
                            assertThat(uuids).contains(designId);
                            String actualJson1 = rows.get(0).get("JSON", String.class);
                            String actualStatus1 = rows.get(0).get("STATUS", String.class);
                            assertThat(actualJson1).isEqualTo(JSON_1);
                            assertThat(actualStatus1).isEqualTo("CREATED");
                            String actualJson2 = rows.get(1).get("JSON", String.class);
                            String actualStatus2 = rows.get(1).get("STATUS", String.class);
                            assertThat(actualJson2).isNull();
                            assertThat(actualStatus2).isEqualTo("DELETED");
                        }
                    });

            await().atMost(FIVE_SECONDS)
                    .pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(message[0]).isNotNull();
                        Message actualMessage = Json.decodeValue(message[0], Message.class);
                        assertThat(actualMessage.getTimestamp()).isNotNull();
                        assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                        assertThat(actualMessage.getPartitionKey()).isEqualTo(designId.toString());
                        assertThat(actualMessage.getMessageId()).isNotNull();
                        assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                        DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChangedEvent.class);
                        assertThat(actualEvent.getUuid()).isEqualTo(designId);
                        assertThat(actualEvent.getTimestamp()).isNotNull();
                        assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp);
                    });
        } finally {
            if (consumer != null) {
                consumer.close();
            }
            if (producer != null) {
                producer.close();
            }
        }
    }

    private void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
    }

    private JsonObject createCassandraConfig() {
        final JsonObject config = new JsonObject();
        config.put("cassandra_contactPoint", System.getProperty("cassandra.host", "localhost"));
        config.put("cassandra_port", 9042);
        config.put("cassandra_cluster", "cassandra");
        config.put("cassandra_username", "admin");
        config.put("cassandra_password", "password");
        return config;
    }

    private JsonObject createProducerConfig() {
        final JsonObject config = new JsonObject();
        config.put("kafka_bootstrapServers", System.getProperty("kafka.host", "localhost") + ":9092");
        return config;
    }

    private JsonObject createConsumerConfig(String group) {
        final JsonObject config = new JsonObject();
        config.put("kafka_bootstrapServers", System.getProperty("kafka.host", "localhost") + ":9092");
        config.put("kafka_group_id", group);
        return config;
    }

    private KafkaProducerRecord<String, String> createKafkaRecord(Message message) {
        return KafkaProducerRecord.create("designs-events", message.getPartitionKey(), Json.encode(message));
    }

    private Message createInsertDesignMessage(UUID messageId, UUID partitionKey, long timestamp, InsertDesignCommand event) {
        return new Message(messageId.toString(), MessageType.DESIGN_INSERT, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    private Message createUpdateDesignMessage(UUID messageId, UUID partitionKey, long timestamp, UpdateDesignCommand event) {
        return new Message(messageId.toString(), MessageType.DESIGN_UPDATE, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    private Message createDeleteDesignMessage(UUID messageId, UUID partitionKey, long timestamp, DeleteDesignCommand event) {
        return new Message(messageId.toString(), MessageType.DESIGN_DELETE, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    private URL makeBaseURL(String path) throws MalformedURLException {
        final Integer port = Integer.getInteger("http.port", 3011);
        final String normPath = path.startsWith("/") ? path.substring(1) : path;
        final String host = System.getProperty("http.host", "localhost");
        return new URL("https://" + host + ":" + port + "/" + normPath);
    }
}
