package com.nextbreakpoint.blueprint.designs;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;
import com.jayway.restassured.RestAssured;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClusterFactory;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.command.DeleteDesign;
import com.nextbreakpoint.blueprint.common.core.command.InsertDesign;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import com.nextbreakpoint.blueprint.common.test.KubeUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@Tag("integration")
@DisplayName("Verify contract of service Designs Command Consumer")
public class IntegrationTests {
    private static final String JSON_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    private static final String JSON_2 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 100] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";

    private static final String version = "1.0.0-1";
    private static final String namespace = "integration";
    private static final long timestamp = System.currentTimeMillis();

    private static final String githubUsername = TestUtils.getVariable("GITHUB_USERNAME");
    private static final String githubPassword = TestUtils.getVariable("GITHUB_PASSWORD");

    private static boolean buildDockerImages = TestUtils.getVariable("BUILD_IMAGES", "false").equals("true");

    private static String httpPort = TestUtils.getVariable("HTTP_PORT", System.getProperty("http.port", "8080"));
    private static String kafkaPort = TestUtils.getVariable("KAFKA_PORT", System.getProperty("kafka.port", "9093"));
    private static String cassandraPort = TestUtils.getVariable("CASSANDRA_PORT", System.getProperty("cassandra.port", "9042"));

    private static String minikubeHost;

    private static Environment environment = Environment.getDefaultEnvironment();

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        printInfo();
        configureMinikube();
        buildDockerImages();
        deleteNamespace();
        createNamespace();
        installCassandra();
        waitForCassandra();
        exposeCassandra();
        installZookeeper();
        waitForZookeeper();
        installKafka();
        waitForKafka();
        exposeKafka();
        createSecrets();
        installServices();
        waitForService();
        exposeService();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        describeResources();
        printLogs();
        uninstallServices();
        uninstallCassandra();
        uninstallKafka();
        uninstallZookeeper();
        deleteNamespace();
    }

    @BeforeEach
    public void setup() {
    }

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
            final Cluster cluster = CassandraClusterFactory.create(environment, createCassandraConfig());

            producer = KafkaUtils.createProducer(environment, createProducerConfig());

            consumer[0] = KafkaUtils.createConsumer(environment, createConsumerConfig("test-insert"));

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
            final Cluster cluster = CassandraClusterFactory.create(environment, createCassandraConfig());

            producer = KafkaUtils.createProducer(environment, createProducerConfig());

            consumer[0] = KafkaUtils.createConsumer(environment, createConsumerConfig("test-update"));

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
            final Cluster cluster = CassandraClusterFactory.create(environment, createCassandraConfig());

            producer = KafkaUtils.createProducer(environment, createProducerConfig());

            consumer[0] = KafkaUtils.createConsumer(environment, createConsumerConfig("test-delete"));

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

    private JsonObject createCassandraConfig() {
        final JsonObject config = new JsonObject();
        config.put("cassandra_contactPoints", minikubeHost);
        config.put("cassandra_port", cassandraPort);
        config.put("cassandra_cluster", "cassandra");
        config.put("cassandra_username", "admin");
        config.put("cassandra_password", "password");
        return config;
    }

    private JsonObject createProducerConfig() {
        final JsonObject config = new JsonObject();
        config.put("kafka_bootstrap_servers", minikubeHost + ":" + kafkaPort);
        config.put("kafka_client_id", "integration");
        return config;
    }

    private JsonObject createConsumerConfig(String group) {
        final JsonObject config = new JsonObject();
        config.put("kafka_bootstrap_servers", minikubeHost + ":" + kafkaPort);
        config.put("kafka_group_id", group);
        return config;
    }

    private ProducerRecord<String, String> createKafkaRecord(Message message) {
        return new ProducerRecord("designs-events", message.getPartitionKey(), Json.encode(message));
    }

    private void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
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

    private static void printInfo() {
        System.out.println("Run test - " + new Date(timestamp));
        System.out.println("Namespace = " + namespace);
        System.out.println("Version = " + version);
        System.out.println("Build image = " + (buildDockerImages ? "Yes" : "No"));
    }

    private static void printLogs() throws IOException, InterruptedException {
        KubeUtils.printLogs(namespace, "designs-command-consumer");
    }

    private static void describeResources() throws IOException, InterruptedException {
        KubeUtils.describePods(namespace);
    }

    private static void createNamespace() throws IOException, InterruptedException {
        if (KubeUtils.createNamespace(namespace) != 0) {
            fail("Can't create namespace");
        }
    }

    private static void deleteNamespace() throws IOException, InterruptedException {
        if (KubeUtils.deleteNamespace(namespace) != 0) {
            System.out.println("Can't delete namespace");
        }
    }

    private static void buildDockerImages() throws IOException, InterruptedException {
        if (!buildDockerImages) {
            return;
        }
        KubeUtils.cleanDockerImages();
        System.out.println("Building image...");
        List<String> args = Arrays.asList(
                "--build-arg", "github_username=" + githubUsername,
                "--build-arg", "github_password=" + githubPassword
        );
        if (KubeUtils.buildDockerImage(".", "integration/designs-command-consumer:" + version, args) != 0) {
            fail("Can't build image");
        }
        System.out.println("Image created");
        buildDockerImages = false;
    }

    private static void installServices() throws IOException, InterruptedException {
        installService("designs-command-consumer");
    }

    private static void uninstallServices() throws IOException, InterruptedException {
        uninstallService("designs-command-consumer");
    }

    private static void installService(String name) throws IOException, InterruptedException {
        System.out.println("Installing service...");
        final List<String> args = Arrays.asList("--set=replicas=1,clientDomain=" + minikubeHost + ",image.pullPolicy=Never,image.repository=integration/" + name + ",image.tag=" + version);
        if (KubeUtils.installHelmChart(namespace, "integration-" + name, "helm", args, true) != 0) {
            if (KubeUtils.upgradeHelmChart(namespace, "integration-" + name, "helm", args, true) != 0) {
                fail("Can't install or upgrade Helm chart");
            }
        }
        System.out.println("Service installed");
    }

    private static void uninstallService(String name) throws IOException, InterruptedException {
        System.out.println("Uninstalling service...");
        if (KubeUtils.uninstallHelmChart(namespace, "integration-" + name) != 0) {
            System.out.println("Can't uninstall Helm chart");
        }
        System.out.println("Service uninstalled");
    }

    private static void waitForService() {
        awaitUntilCondition(20, 5, 1, () -> isServiceReady(namespace, "designs-command-consumer"));
    }

    private static boolean isServiceReady(String namespace, String name) throws IOException, InterruptedException {
        String logs = KubeUtils.fetchLogs(namespace, name);
        String[] lines = logs.split("\n");
        boolean serviceReady = Arrays.stream(lines).anyMatch(line -> line.contains("Service listening on port 8080"));
        return serviceReady;
    }

    private static void createSecrets() throws IOException, InterruptedException {
        System.out.println("Creating secrets...");
        final List<String> args = Arrays.asList(
                "--from-file",
                "keystore_server.jks=../../secrets/keystore_server.jks",
                "--from-file",
                "keystore_auth.jceks=../../secrets/keystore_auth.jceks",
                "--from-literal",
                "KEYSTORE_SECRET=secret",
                "--from-literal",
                "DATABASE_USERNAME=verticle",
                "--from-literal",
                "DATABASE_PASSWORD=password"
        );
        if (KubeUtils.createSecret(namespace,"designs-command-consumer", args, true) != 0) {
            fail("Can't create secret");
        }
        System.out.println("Secrets created");
    }

    private static void exposeService() throws IOException, InterruptedException {
        System.out.println("Exposing service...");
        if (KubeUtils.exposeService(namespace,"designs-command-consumer", Integer.parseInt(httpPort), 8080) != 0) {
            fail("Can't expose service");
        }
        System.out.println("Service exposed");
    }

    private static void installZookeeper() throws IOException, InterruptedException {
        System.out.println("Installing Zookeeper...");
        final List<String> args = Arrays.asList("--set=replicas=1");
        if (KubeUtils.installHelmChart(namespace, "integration-zookeeper", "../../helm/zookeeper", args, true) != 0) {
            if (KubeUtils.upgradeHelmChart(namespace, "integration-zookeeper", "../../helm/zookeeper", args, true) != 0) {
                fail("Can't install or upgrade Helm chart");
            }
        }
        System.out.println("Zookeeper installed");
    }

    private static void uninstallZookeeper() throws IOException, InterruptedException {
        System.out.println("Uninstalling Zookeeper...");
        if (KubeUtils.uninstallHelmChart(namespace, "integration-zookeeper") != 0) {
            System.out.println("Can't uninstall Helm chart");
        }
        System.out.println("Zookeeper uninstalled");
    }

    private static void waitForZookeeper() {
        awaitUntilCondition(60, 10, 5, () -> isZookeeperReady(namespace));
    }

    private static boolean isZookeeperReady(String namespace) throws IOException, InterruptedException {
        String logs = KubeUtils.fetchLogs(namespace, "zookeeper");
        String[] lines = logs.split("\n");
        boolean serverReady = Arrays.stream(lines).anyMatch(line -> line.contains("binding to port /0.0.0.0:2181"));
        return serverReady;
    }

    private static void installKafka() throws IOException, InterruptedException {
        System.out.println("Installing Kafka...");
        final List<String> args = Arrays.asList("--set=replicas=1,externalName=" + minikubeHost + ",externalPort=" + kafkaPort);
        if (KubeUtils.installHelmChart(namespace, "integration-kafka", "../../helm/kafka", args, true) != 0) {
            if (KubeUtils.upgradeHelmChart(namespace, "integration-kafka", "../../helm/kafka", args, true) != 0) {
                fail("Can't install or upgrade Helm chart");
            }
        }
        System.out.println("Kafka installed");
    }

    private static void uninstallKafka() throws IOException, InterruptedException {
        System.out.println("Uninstalling Kafka...");
        if (KubeUtils.uninstallHelmChart(namespace, "integration-kafka") != 0) {
            System.out.println("Can't uninstall Helm chart");
        }
        System.out.println("Kafka uninstalled");
    }

    private static void waitForKafka() {
        awaitUntilCondition(60, 10, 5, () -> isKafkaReady(namespace));
    }

    private static boolean isKafkaReady(String namespace) throws IOException, InterruptedException {
        String logs = KubeUtils.fetchLogs(namespace, "kafka");
        String[] lines = logs.split("\n");
        boolean serverReady = Arrays.stream(lines).anyMatch(line -> line.contains("[KafkaServer id=1] started (kafka.server.KafkaServer)"));
        return serverReady;
    }

    private static void exposeKafka() throws IOException, InterruptedException {
        System.out.println("Exposing Kafka...");
        if (exposeService(namespace,"kafka", Integer.parseInt(kafkaPort), 9093) != 0) {
            fail("Can't expose Kafka");
        }
        System.out.println("Kafka exposed");
    }

    public static int exposeService(String namespace, String name, int port, int targetPort) throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "sh",
                "-c",
                "kubectl -n " + namespace + " expose service " + name + " --type=LoadBalancer --name=" + name + "-lb --port=" + port + " --target-port=" + targetPort + " --external-ip=$(minikube ip)"
        );
        return KubeUtils.executeCommand(command, true);
    }

    private static void installCassandra() throws IOException, InterruptedException {
        System.out.println("Installing Cassandra...");
        final List<String> args = Arrays.asList("--set=replicas=1");
        if (KubeUtils.installHelmChart(namespace, "integration-cassandra", "../../helm/cassandra", args, true) != 0) {
            if (KubeUtils.upgradeHelmChart(namespace, "integration-cassandra", "../../helm/cassandra", args, true) != 0) {
                fail("Can't install or upgrade Helm chart");
            }
        }
        System.out.println("Cassandra installed");
    }

    private static void uninstallCassandra() throws IOException, InterruptedException {
        System.out.println("Uninstalling Cassandra...");
        if (KubeUtils.uninstallHelmChart(namespace, "integration-cassandra") != 0) {
            System.out.println("Can't uninstall Helm chart");
        }
        System.out.println("Cassandra uninstalled");
    }

    private static void waitForCassandra() {
        awaitUntilCondition(90, 30, 10, () -> isCassandraReady(namespace));
    }

    private static boolean isCassandraReady(String namespace) throws IOException, InterruptedException {
        String logs = KubeUtils.fetchLogs(namespace, "cassandra");
        String[] lines = logs.split("\n");
        boolean serverReady = Arrays.stream(lines).anyMatch(line -> line.contains("Truncate of designs.designs is complete"));
        return serverReady;
    }

    private static void exposeCassandra() throws IOException, InterruptedException {
        System.out.println("Exposing Cassandra...");
        if (exposeService(namespace,"cassandra", Integer.parseInt(cassandraPort), Integer.parseInt(cassandraPort)) != 0) {
            fail("Can't expose Cassandra");
        }
        System.out.println("Cassandra exposed");
    }

    private static void configureMinikube() throws IOException, InterruptedException {
        minikubeHost = KubeUtils.getMinikubeIp();
    }

    public static void awaitUntilAsserted(Long timeout, Long delay, Long interval, ThrowingRunnable assertion) {
        Awaitility.await()
                .atMost(timeout, TimeUnit.SECONDS)
                .pollDelay(delay, TimeUnit.SECONDS)
                .pollInterval(interval, TimeUnit.SECONDS)
                .untilAsserted(assertion);
    }

    public static void awaitUntilCondition(int timeout, int delay, int interval, Callable<Boolean> condition) {
        Awaitility.await()
                .atMost(timeout, TimeUnit.SECONDS)
                .pollDelay(delay, TimeUnit.SECONDS)
                .pollInterval(interval, TimeUnit.SECONDS)
                .until(condition);
    }
}
