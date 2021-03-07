package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Design;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.command.DeleteDesign;
import com.nextbreakpoint.blueprint.common.core.command.InsertDesign;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import com.nextbreakpoint.blueprint.common.test.KubeUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@Tag("integration")
@DisplayName("Verify contract of service Designs Command Producer")
public class IntegrationTests {
    private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
    private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
    private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";

    private static final String KEYSTORE_AUTH_JCEKS_PATH = "../../secrets/keystore_auth.jceks";

    private static final String version = "1.0.0-1";
    private static final String namespace = "integration";
    private static final long timestamp = System.currentTimeMillis();

    private static final String githubUsername = TestUtils.getVariable("GITHUB_USERNAME");
    private static final String githubPassword = TestUtils.getVariable("GITHUB_PASSWORD");

    private static boolean buildDockerImages = TestUtils.getVariable("BUILD_IMAGES", "false").equals("true");

    private static String httpPort = TestUtils.getVariable("HTTP_PORT", System.getProperty("http.port", "8080"));
    private static String kafkaPort = TestUtils.getVariable("KAFKA_PORT", System.getProperty("kafka.port", "9093"));

    private static String minikubeHost;

    private static RestAssuredConfig restAssuredConfig;

    private static Environment environment = Environment.getDefaultEnvironment();

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        printInfo();
        configureMinikube();
        buildDockerImages();
        configureRestAssured();
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
    }

    @AfterEach
    public void reset() {
        RestAssured.reset();
    }

    @Test
    @DisplayName("Should allow OPTIONS on /v1/designs without access token")
    public void shouldAllowOptionsOnDesignsWithoutAccessToken() throws MalformedURLException {
        given().config(restAssuredConfig)
                .with().header("Origin", "https://" + minikubeHost + ":" + httpPort)
                .when().options(makeBaseURL("/v1/designs"))
                .then().assertThat().statusCode(204)
                .and().header("Access-Control-Allow-Origin", "https://" + minikubeHost + ":" + httpPort)
                .and().header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Should allow OPTIONS on /v1/designs/id without access token")
    public void shouldAllowOptionsOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
        given().config(restAssuredConfig)
                .with().header("Origin", "https://" + minikubeHost + ":" + httpPort)
                .when().options(makeBaseURL("/v1/designs/" + UUID.randomUUID().toString()))
                .then().assertThat().statusCode(204)
                .and().header("Access-Control-Allow-Origin", "https://" + minikubeHost + ":" + httpPort)
                .and().header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Should forbid POST on /v1/designs without access token")
    public void shouldForbidPostOnDesignsWithoutAccessToken() throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(createPostData())
                .when().post(makeBaseURL("/v1/designs"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid PUT on /v1/designs/id without access token")
    public void shouldForbidPutOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(createPostData())
                .when().put(makeBaseURL("/v1/designs/" + UUID.randomUUID().toString()))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid DELETE on /v1/designs/id without access token")
    public void shouldForbidDeleteOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
        final String uuid = UUID.randomUUID().toString();

        given().config(restAssuredConfig)
                .and().accept(ContentType.JSON)
                .when().delete(makeBaseURL("/v1/designs/" + uuid))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should send a message after accepting POST on /v1/designs")
    public void shouldAcceptPostOnDesigns() throws IOException, InterruptedException {
        final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

        pause(100);

        final KafkaConsumer<String, String>[] consumer = new KafkaConsumer[1];

        Thread polling = null;

        try {
            consumer[0] = KafkaUtils.createConsumer(environment, createConsumerConfig("test-insert"));

            consumer[0].subscribe(Collections.singleton("designs-events"));

            long timestamp = System.currentTimeMillis();

            final List<ConsumerRecord<String, String>> records = new ArrayList<>();

            polling = createConsumerThread(records, consumer[0]);

            polling.start();

            createDesign(authorization, createPostData());

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final Optional<Message> message = safelyFindMessage(records, "design-insert");
                        assertThat(message.isEmpty()).isFalse();
                        Message decodedMessage = message.get();
                        assertThat(decodedMessage.getMessageType()).isEqualTo("design-insert");
                        assertThat(decodedMessage.getMessageId()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isNotNull();
                        assertThat(decodedMessage.getMessageSource()).isNotNull();
                        assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                        InsertDesign decodedEvent = Json.decodeValue(decodedMessage.getMessageBody(), InsertDesign.class);
                        assertThat(decodedEvent.getUuid()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isEqualTo(decodedEvent.getUuid().toString());
                        Design decodedDesign = Json.decodeValue(decodedEvent.getJson(), Design.class);
                        assertThat(decodedDesign.getManifest()).isEqualTo(MANIFEST);
                        assertThat(decodedDesign.getMetadata()).isEqualTo(METADATA);
                        assertThat(decodedDesign.getScript()).isEqualTo(SCRIPT);
                    });
        } finally {
            if (polling != null) {
                polling.interrupt();
                polling.join();
            }
            if (consumer[0] != null) {
                consumer[0].close();
            }
        }
    }

    @Test
    @DisplayName("Should send a message after accepting PUT on /v1/designs/id")
    public void shouldAcceptPutOnDesignsSlashId() throws IOException, InterruptedException {
        final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

        pause(100);

        final KafkaConsumer<String, String>[] consumer = new KafkaConsumer[1];

        Thread polling = null;

        try {
            consumer[0] = KafkaUtils.createConsumer(environment, createConsumerConfig("test-update"));

            consumer[0].subscribe(Collections.singleton("designs-events"));

            final String uuid = UUID.randomUUID().toString();

            long timestamp = System.currentTimeMillis();

            final List<ConsumerRecord<String, String>> records = new ArrayList<>();

            polling = createConsumerThread(records, consumer[0]);

            polling.start();

            updateDesign(authorization, createPostData(), uuid);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final Optional<Message> message = safelyFindMessage(records, "design-update");
                        assertThat(message.isEmpty()).isFalse();
                        Message decodedMessage = message.get();
                        assertThat(decodedMessage.getMessageType()).isEqualTo("design-update");
                        assertThat(decodedMessage.getMessageId()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isEqualTo(uuid);
                        assertThat(decodedMessage.getMessageSource()).isNotNull();
                        assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                        UpdateDesign decodedEvent = Json.decodeValue(decodedMessage.getMessageBody(), UpdateDesign.class);
                        assertThat(decodedEvent.getUuid()).isNotNull();
                        assertThat(decodedEvent.getUuid().toString()).isEqualTo(uuid);
                        Design decodedDesign = Json.decodeValue(decodedEvent.getJson(), Design.class);
                        assertThat(decodedDesign.getManifest()).isEqualTo(MANIFEST);
                        assertThat(decodedDesign.getMetadata()).isEqualTo(METADATA);
                        assertThat(decodedDesign.getScript()).isEqualTo(SCRIPT);
                    });
        } finally {
            if (polling != null) {
                polling.interrupt();
                polling.join();
            }
            if (consumer[0] != null) {
                consumer[0].close();
            }
        }
    }

    @Test
    @DisplayName("Should send a message after accepting DELETE on /v1/designs/id")
    public void shouldAcceptDeleteOnDesignsSlashId() throws IOException, InterruptedException {
        final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

        pause(100);

        final KafkaConsumer<String, String>[] consumer = new KafkaConsumer[1];

        Thread polling = null;

        try {
            consumer[0] = KafkaUtils.createConsumer(environment, createConsumerConfig("test-delete"));

            consumer[0].subscribe(Collections.singleton("designs-events"));

            final String uuid = UUID.randomUUID().toString();

            long timestamp = System.currentTimeMillis();

            final List<ConsumerRecord<String, String>> records = new ArrayList<>();

            polling = createConsumerThread(records, consumer[0]);

            polling.start();

            deleteDesign(authorization, uuid);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final Optional<Message> message = safelyFindMessage(records, "design-delete");
                        assertThat(message.isEmpty()).isFalse();
                        Message decodedMessage = message.get();
                        assertThat(decodedMessage.getMessageType()).isEqualTo("design-delete");
                        assertThat(decodedMessage.getMessageId()).isNotNull();
                        assertThat(decodedMessage.getPartitionKey()).isEqualTo(uuid);
                        assertThat(decodedMessage.getMessageSource()).isNotNull();
                        assertThat(decodedMessage.getTimestamp()).isGreaterThan(timestamp);
                        DeleteDesign decodedEvent = Json.decodeValue(decodedMessage.getMessageBody(), DeleteDesign.class);
                        assertThat(decodedEvent.getUuid()).isNotNull();
                        assertThat(decodedEvent.getUuid().toString()).isEqualTo(uuid);
                    });
        } finally {
            if (polling != null) {
                polling.interrupt();
                polling.join();
            }
            if (consumer[0] != null) {
                consumer[0].close();
            }
        }
    }

    private Optional<Message> safelyFindMessage(List<ConsumerRecord<String, String>> records, String messageType) {
        synchronized (records) {
            return records.stream()
                    .map(record -> Json.decodeValue(record.value(), Message.class))
                    .filter(message -> message.getMessageType().equals(messageType))
                    .findFirst();
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

    private void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    private void deleteDesign(String authorization, String uuid) throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().header(AUTHORIZATION, authorization)
                .and().accept(ContentType.JSON)
                .when().delete(makeBaseURL("/v1/designs/" + uuid))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
    }

    private void createDesign(String authorization, Map<String, String> design) throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(design)
                .when().post(makeBaseURL("/v1/designs"))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
//            .and().body("messageId", notNullValue())
//            .and().extract().response().body().jsonPath().getString("messageId");
    }

    private void updateDesign(String authorization, Map<String, String> design, String uuid) throws MalformedURLException {
        given().config(restAssuredConfig)
                .and().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(design)
                .when().put(makeBaseURL("/v1/designs/" + uuid))
                .then().assertThat().statusCode(202)
                .and().contentType(ContentType.JSON);
    }

    private JsonObject createConsumerConfig(String group) {
        final JsonObject config = new JsonObject();
        config.put("kafka_bootstrap_servers", minikubeHost + ":" + kafkaPort);
        config.put("kafka_group_id", group);
        return config;
    }

    private Map<String, String> createPostData() {
        final Map<String, String> data = new HashMap<>();
        data.put("manifest", MANIFEST);
        data.put("metadata", METADATA);
        data.put("script", SCRIPT);
        return data;
    }

    private static URL makeBaseURL(String path) throws MalformedURLException {
        final String normPath = path.startsWith("/") ? path.substring(1) : path;
        return new URL("https://" + minikubeHost + ":" + httpPort + "/" + normPath);
    }

    private static void configureRestAssured() {
        final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
        final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
        final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        restAssuredConfig = RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
    }

    private static void printInfo() {
        System.out.println("Run test - " + new Date(timestamp));
        System.out.println("Namespace = " + namespace);
        System.out.println("Version = " + version);
        System.out.println("Build image = " + (buildDockerImages ? "Yes" : "No"));
    }

    private static void printLogs() throws IOException, InterruptedException {
        KubeUtils.printLogs(namespace, "designs-command-producer");
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
        if (KubeUtils.buildDockerImage(".", "integration/designs-command-producer:" + version, args) != 0) {
            fail("Can't build image");
        }
        System.out.println("Image created");
        buildDockerImages = false;
    }

    private static void installServices() throws IOException, InterruptedException {
        installService("designs-command-producer");
    }

    private static void uninstallServices() throws IOException, InterruptedException {
        uninstallService("designs-command-producer");
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
        awaitUntilCondition(20, 5, 1, () -> isServiceReady(namespace, "designs-command-producer"));
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
        if (KubeUtils.createSecret(namespace,"designs-command-producer", args, true) != 0) {
            fail("Can't create secret");
        }
        System.out.println("Secrets created");
    }

    private static void exposeService() throws IOException, InterruptedException {
        System.out.println("Exposing service...");
        if (KubeUtils.exposeService(namespace,"designs-command-producer", Integer.parseInt(httpPort), 8080) != 0) {
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
