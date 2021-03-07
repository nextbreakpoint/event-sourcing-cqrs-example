package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.EventSource;
import com.nextbreakpoint.blueprint.common.test.KubeUtils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@Tag("integration")
@DisplayName("Verify contract of service Designs Notification Dispatcher")
public class IntegrationTests {
    private static final String version = "1.0.0-1";
    private static final String namespace = "integration";
    private static final long timestamp = System.currentTimeMillis();

    private static final String githubUsername = TestUtils.getVariable("GITHUB_USERNAME");
    private static final String githubPassword = TestUtils.getVariable("GITHUB_PASSWORD");

    private static boolean buildDockerImages = TestUtils.getVariable("BUILD_IMAGES", "false").equals("true");

    private static String httpPort = TestUtils.getVariable("HTTP_PORT", System.getProperty("http.port", "8080"));
    private static String kafkaPort = TestUtils.getVariable("KAFKA_PORT", System.getProperty("kafka.port", "9093"));

    private static String minikubeHost;

    private static Environment environment = Environment.getDefaultEnvironment();

    private EventSource eventSource;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        printInfo();
        configureMinikube();
        buildDockerImages();
        deleteNamespace();
        createNamespace();
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
        uninstallKafka();
        uninstallZookeeper();
        deleteNamespace();
    }

    @BeforeEach
    public void setup() {
        final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());
        final JsonObject config = new JsonObject();
        config.put("client_keep_alive", "true");
        config.put("client_verify_host", "false");
        config.put("client_keystore_path", "../../secrets/keystore_client.jks");
        config.put("client_keystore_secret", "secret");
        config.put("client_truststore_path", "../../secrets/truststore_client.jks");
        config.put("client_truststore_secret", "secret");
        eventSource = new EventSource(environment, vertx, "https://" + minikubeHost + ":" + httpPort, config);
    }

    @AfterEach
    public void reset() {
        if (eventSource != null) {
            eventSource.close();
        }
    }

    @Test
    @DisplayName("Should notify watchers after receiving a DesignChanged event")
    public void shouldNotifyWatchersWhenRecevingAnEvent() throws IOException {
        KafkaProducer<String, String> producer[] = new KafkaProducer[1];

        try {
            producer[0] = KafkaUtils.createProducer(environment, createProducerConfig());

            long eventTimestamp = System.currentTimeMillis();

            final UUID messageId = UUID.randomUUID();
            final UUID designId = UUID.randomUUID();

            final DesignChanged designChangedEvent = new DesignChanged(designId, eventTimestamp);

            final long messageTimestamp = System.currentTimeMillis();

            final Message designChangedMessage = createDesignChangedMessage(messageId, designId, messageTimestamp, designChangedEvent);

            final Boolean[] connected = new Boolean[]{null};
            final String[] open = new String[]{null};
            final String[] update = new String[]{null};

            eventSource.connect("/v1/sse/designs/0", null, result -> {
                connected[0] = result.succeeded();
                producer[0].send(createKafkaRecord(designChangedMessage));
            }).onEvent("update", sseEvent -> {
                update[0] = sseEvent;
                System.out.println(sseEvent);
            }).onEvent("open", sseEvent -> {
                open[0] = sseEvent;
                System.out.println(sseEvent);
            }).onClose(nothing -> {});

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        assertThat(connected[0]).isNotNull();
                        assertThat(connected[0]).isTrue();
                        assertThat(open[0]).isNotNull();
                        String openData = open[0].split("\n")[1];
                        Map<String, Object> openObject = Json.decodeValue(openData, HashMap.class);
                        assertThat(openObject.get("session")).isNotNull();
                        assertThat(update[0]).isNotNull();
                        String updateData = update[0].split("\n")[1];
                        Map<String, Object> updateObject = Json.decodeValue(updateData, HashMap.class);
                        assertThat(updateObject.get("session")).isNotNull();
                        assertThat(updateObject.get("session")).isEqualTo(openObject.get("session"));
                        assertThat(updateObject.get("uuid")).isNotNull();
                        assertThat(updateObject.get("uuid")).isEqualTo("*");
                    });
        } finally {
            if (producer[0] != null) {
                producer[0].close();
            }
        }
    }

    @Test
    @DisplayName("Should notify watchers after receiving a DesignChanged event for single design")
    public void shouldNotifyWatchersWhenRecevingAnEventForSingleDesign() throws IOException {
        KafkaProducer<String, String> producer[] = new KafkaProducer[1];

        try {
            producer[0] = KafkaUtils.createProducer(environment, createProducerConfig());

            long eventTimestamp = System.currentTimeMillis();

            final UUID messageId = UUID.randomUUID();
            final UUID designId = UUID.randomUUID();

            final DesignChanged designChangedEvent = new DesignChanged(designId, eventTimestamp);

            final long messageTimestamp = System.currentTimeMillis();

            final Message designChangedMessage = createDesignChangedMessage(messageId, designId, messageTimestamp, designChangedEvent);

            final Boolean[] connected = new Boolean[]{null};
            final String[] open = new String[]{null};
            final String[] update = new String[]{null};

            eventSource.connect("/v1/sse/designs/0/" + designId, null, result -> {
                connected[0] = result.succeeded();
                producer[0].send(createKafkaRecord(designChangedMessage));
            }).onEvent("update", sseEvent -> {
                update[0] = sseEvent;
                System.out.println(sseEvent);
            }).onEvent("open", sseEvent -> {
                open[0] = sseEvent;
                System.out.println(sseEvent);
            }).onClose(nothing -> {});

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        assertThat(connected[0]).isNotNull();
                        assertThat(connected[0]).isTrue();
                        assertThat(open[0]).isNotNull();
                        String openData = open[0].split("\n")[1];
                        Map<String, Object> openObject = Json.decodeValue(openData, HashMap.class);
                        assertThat(openObject.get("session")).isNotNull();
                        assertThat(update[0]).isNotNull();
                        String updateData = update[0].split("\n")[1];
                        Map<String, Object> updateObject = Json.decodeValue(updateData, HashMap.class);
                        assertThat(updateObject.get("session")).isNotNull();
                        assertThat(updateObject.get("session")).isEqualTo(openObject.get("session"));
                        assertThat(updateObject.get("uuid")).isNotNull();
                        assertThat(updateObject.get("uuid")).isEqualTo(designId.toString());
                    });
        } finally {
            if (producer[0] != null) {
                producer[0].close();
            }
        }
    }

    private JsonObject createProducerConfig() {
        final JsonObject config = new JsonObject();
        config.put("kafka_bootstrap_servers", minikubeHost + ":" + kafkaPort);
        config.put("kafka_client_id", "integration");
        return config;
    }

    private ProducerRecord<String, String> createKafkaRecord(Message message) {
        return new ProducerRecord("designs-sse", message.getPartitionKey(), Json.encode(message));
    }

    private Message createDesignChangedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChanged event) {
        return new Message(messageId.toString(), MessageType.DESIGN_CHANGED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    private static void printInfo() {
        System.out.println("Run test - " + new Date(timestamp));
        System.out.println("Namespace = " + namespace);
        System.out.println("Version = " + version);
        System.out.println("Build image = " + (buildDockerImages ? "Yes" : "No"));
    }

    private static void printLogs() throws IOException, InterruptedException {
        KubeUtils.printLogs(namespace, "designs-notification-dispatcher");
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
        if (KubeUtils.buildDockerImage(".", "integration/designs-notification-dispatcher:" + version, args) != 0) {
            fail("Can't build image");
        }
        System.out.println("Image created");
        buildDockerImages = false;
    }

    private static void installServices() throws IOException, InterruptedException {
        installService("designs-notification-dispatcher");
    }

    private static void uninstallServices() throws IOException, InterruptedException {
        uninstallService("designs-notification-dispatcher");
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
        awaitUntilCondition(20, 5, 1, () -> isServiceReady(namespace, "designs-notification-dispatcher"));
    }

    private static boolean isServiceReady(String namespace, String name) throws IOException, InterruptedException {
        String logs = KubeUtils.fetchLogs(namespace, name);
        String[] lines = logs.split("\n");
        boolean serviceReady = Arrays.stream(lines).anyMatch(line -> line.contains("Succeeded in deploying verticle"));
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
                "KEYSTORE_SECRET=secret"
        );
        if (KubeUtils.createSecret(namespace,"designs-notification-dispatcher", args, true) != 0) {
            fail("Can't create secret");
        }
        System.out.println("Secrets created");
    }

    private static void exposeService() throws IOException, InterruptedException {
        System.out.println("Exposing service...");
        if (KubeUtils.exposeService(namespace,"designs-notification-dispatcher", Integer.parseInt(httpPort), 8080) != 0) {
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
