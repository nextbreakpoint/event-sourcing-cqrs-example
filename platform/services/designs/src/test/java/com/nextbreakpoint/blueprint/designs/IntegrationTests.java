package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.DesignDocument;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import com.nextbreakpoint.blueprint.common.test.KubeUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.hamcrest.CoreMatchers.notNullValue;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@Tag("integration")
@DisplayName("Designs service")
public class IntegrationTests {
  private static final String SCRIPT1 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String SCRIPT2 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 100] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
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
    installMySQL();
    waitForMySQL();
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
    uninstallMySQL();
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
  @DisplayName("should allow options request without access token")
  public void shouldAllowOptionsRequestWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .with().header("Origin", "https://" + minikubeHost + ":" + httpPort)
            .when().options(makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://" + minikubeHost + ":" + httpPort)
            .and().header("Access-Control-Allow-Credentials", "true");

    given().config(restAssuredConfig)
            .with().header("Origin", "https://" + minikubeHost + ":" + httpPort)
            .when().options(makeBaseURL("/v1/designs/" + UUID.randomUUID().toString()))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://" + minikubeHost + ":" + httpPort)
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("should forbid post request without access token")
  public void shouldForbidPostRequestWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(createPostData(SCRIPT1))
            .when().post(makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid delete request without access token")
  public void shouldForbidDeleteRequestWithoutAccessToken() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    final String uuid = createDesign(authorization, createPostData(SCRIPT1));

    given().config(restAssuredConfig)
            .and().accept(ContentType.JSON)
            .when().delete(makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid get request when user doesn't have permissions")
  public void shouldForbidGetRequestWhenUserDoNotHavePermissions() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    final String uuid = createDesign(authorization, createPostData(SCRIPT1));

    final String otherAuthorization = VertxUtils.makeAuthorization("test", Arrays.asList("other"), KEYSTORE_AUTH_JCEKS_PATH);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/v1/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should allow get request when user is anonymous")
  public void shouldAllowGetRequestWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    final String uuid = createDesign(authorization, createPostData(SCRIPT1));

    pause();

    given().config(restAssuredConfig)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(200);

    given().config(restAssuredConfig)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(200);

    given().config(restAssuredConfig)
            .and().accept("image/png")
            .when().get(makeBaseURL("/v1/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(200);
  }

  @Test
  @DisplayName("should create and delete designs")
  public void shouldCreateAndDeleteDesigns() throws IOException, InterruptedException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    Thread polling = null;

    pause();

    KafkaConsumer<String, String> consumer[] = new KafkaConsumer[1];

    try {
      consumer[0] = KafkaUtils.createConsumer(environment, createConsumerConfig("test"));

      consumer[0].subscribe(Collections.singleton("designs-sse"));

      long eventTimestamp0 = System.currentTimeMillis();

      final String uuid1 = createDesign(authorization, createPostData(SCRIPT1));

      final List<ConsumerRecord<String, String>> records = new ArrayList<>();

      polling = createConsumerThread(records, consumer[0]);

      polling.start();

      await().atMost(TEN_SECONDS)
              .pollInterval(ONE_SECOND)
              .untilAsserted(() -> {
                final Optional<Message> message = safelyFindMessage(records, UUID.fromString(uuid1));
                assertThat(message.isEmpty()).isFalse();
                Message actualMessage = message.get();
                assertThat(actualMessage.getTimestamp()).isNotNull();
                assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                assertThat(actualMessage.getPartitionKey()).isEqualTo(uuid1);
                assertThat(actualMessage.getMessageId()).isNotNull();
                assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                DesignChanged actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChanged.class);
                assertThat(actualEvent.getUuid()).isEqualTo(UUID.fromString(uuid1));
                assertThat(actualEvent.getTimestamp()).isNotNull();
                assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp0);
              });

      clearRecords(records);

      pause();

      final JsonPath jsonPath0 = getDesign(authorization, uuid1);

      assertThat(jsonPath0.getString("uuid")).isEqualTo(uuid1);
      assertThat(jsonPath0.getString("json")).isNotNull();
      assertThat(jsonPath0.getString("modified")).isNotNull();
      assertThat(jsonPath0.getString("checksum")).isNotNull();

      final String json0 = jsonPath0.getString("json");
      System.out.println(json0);

      final Map<String, Object> object0 = Json.decodeValue(json0, HashMap.class);
      assertThat(object0.get("script")).isEqualTo(SCRIPT1);
      assertThat(object0.get("metadata")).isEqualTo(METADATA);
      assertThat(object0.get("manifest")).isEqualTo(MANIFEST);

      long eventTimestamp1 = System.currentTimeMillis();

      updateDesign(authorization, uuid1, createPostData(SCRIPT2));

      pause();

      await().atMost(TEN_SECONDS)
              .pollInterval(ONE_SECOND)
              .untilAsserted(() -> {
                final Optional<Message> message = safelyFindMessage(records, UUID.fromString(uuid1));
                assertThat(message.isEmpty()).isFalse();
                Message actualMessage = message.get();
                assertThat(actualMessage.getTimestamp()).isNotNull();
                assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                assertThat(actualMessage.getPartitionKey()).isEqualTo(uuid1);
                assertThat(actualMessage.getMessageId()).isNotNull();
                assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                DesignChanged actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChanged.class);
                assertThat(actualEvent.getUuid()).isEqualTo(UUID.fromString(uuid1));
                assertThat(actualEvent.getTimestamp()).isNotNull();
                assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp1);
              });

      clearRecords(records);

      final JsonPath jsonPath1 = getDesign(authorization, uuid1);

      assertThat(jsonPath1.getString("uuid")).isEqualTo(uuid1);
      assertThat(jsonPath1.getString("json")).isNotNull();
      assertThat(jsonPath1.getString("modified")).isNotNull();
      assertThat(jsonPath1.getString("checksum")).isNotNull();

      final String json1 = jsonPath1.getString("json");
      System.out.println(json1);

      final Map<String, Object> object1 = Json.decodeValue(json1, HashMap.class);
      assertThat(object1.get("script")).isEqualTo(SCRIPT2);
      assertThat(object1.get("metadata")).isEqualTo(METADATA);
      assertThat(object1.get("manifest")).isEqualTo(MANIFEST);

      final String uuid2 = createDesign(authorization, createPostData(SCRIPT1));

      pause();

      final JsonPath jsonPath2 = getDesign(authorization, uuid2);

      assertThat(jsonPath2.getString("uuid")).isEqualTo(uuid2);
      assertThat(jsonPath2.getString("json")).isNotNull();
      assertThat(jsonPath2.getString("modified")).isNotNull();
      assertThat(jsonPath2.getString("checksum")).isNotNull();

      final String json2 = jsonPath2.getString("json");
      System.out.println(json2);

      final Map<String, Object> object2 = Json.decodeValue(json2, HashMap.class);
      assertThat(object2.get("script")).isEqualTo(SCRIPT1);
      assertThat(object2.get("metadata")).isEqualTo(METADATA);
      assertThat(object2.get("manifest")).isEqualTo(MANIFEST);

      final DesignDocument document1 = new DesignDocument(uuid1, null, jsonPath1.getString("checksum"), null);
      final DesignDocument document2 = new DesignDocument(uuid2, null, jsonPath2.getString("checksum"), null);

      assertThat(getDesigns(authorization)).contains(document1, document2);

      final byte[] bytes = getTile(authorization, uuid1);

      final BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
      assertThat(image.getWidth()).isEqualTo(256);
      assertThat(image.getHeight()).isEqualTo(256);

      long eventTimestamp3 = System.currentTimeMillis();

      deleteDesign(authorization, uuid1);

      pause();

      await().atMost(TEN_SECONDS)
              .pollInterval(ONE_SECOND)
              .untilAsserted(() -> {
                final Optional<Message> message = safelyFindMessage(records, UUID.fromString(uuid1));
                assertThat(message.isEmpty()).isFalse();
                Message actualMessage = message.get();
                assertThat(actualMessage.getTimestamp()).isNotNull();
                assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                assertThat(actualMessage.getPartitionKey()).isEqualTo(uuid1);
                assertThat(actualMessage.getMessageId()).isNotNull();
                assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                DesignChanged actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChanged.class);
                assertThat(actualEvent.getUuid()).isEqualTo(UUID.fromString(uuid1));
                assertThat(actualEvent.getTimestamp()).isNotNull();
                assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp3);
              });

      clearRecords(records);

      assertThat(getDesigns(authorization)).contains(document2);
      assertThat(getDesigns(authorization)).doesNotContain(document1);

      deleteDesign(authorization, uuid2);

      pause();

      assertThat(getDesigns(authorization)).doesNotContain(document1, document2);
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

  private void clearRecords(List<ConsumerRecord<String, String>> records) {
    synchronized (records) {
      records.clear();
    }
  }

  private Optional<Message> safelyFindMessage(List<ConsumerRecord<String, String>> records, UUID designId) {
    synchronized (records) {
      return records.stream()
              .map(record -> Json.decodeValue(record.value(), Message.class))
              .filter(value -> value.getPartitionKey().equals(designId.toString()))
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
          ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(5000);
          System.out.println("Received " + consumerRecords.count() + " messages");
          consumerRecords.forEach(consumerRecord -> safelyAppendRecord(records, consumerRecord));
          kafkaConsumer.commitSync();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private void pause() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
  }

  private void deleteDesign(String authorization, String uuid) throws MalformedURLException {
    System.out.println("delete design " + uuid);
    given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().delete(makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue());
  }

  private byte[] getTile(String authorization, String uuid) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/v1/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(200)
            .and().contentType("image/png")
            .and().extract().asByteArray();
  }

  private DesignDocument[] getDesigns(String authorization) throws MalformedURLException {
    System.out.println("get designs");
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().body().as(DesignDocument[].class);
  }

  private JsonPath getDesign(String authorization, String uuid) throws MalformedURLException {
    System.out.println("get design " + uuid);
    return given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(200).extract().jsonPath();
  }

  private String createDesign(String authorization, Map<String, String> design) throws MalformedURLException {
    System.out.println("create design");
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(design)
            .when().post(makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(201)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue())
            .and().extract().response().body().jsonPath().getString("uuid");
  }

  private String updateDesign(String authorization, String uuid, Map<String, String> design) throws MalformedURLException {
    System.out.println("update design " + uuid);
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(design)
            .when().put(makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue())
            .and().extract().response().body().jsonPath().getString("uuid");
  }

  private JsonObject createConsumerConfig(String group) {
    final JsonObject config = new JsonObject();
    config.put("kafka_bootstrap_servers", minikubeHost + ":" + kafkaPort);
    config.put("kafka_group_id", group);
    return config;
  }

  private Map<String, String> createPostData(String script) {
    final Map<String, String> data = new HashMap<>();
    data.put("manifest", MANIFEST);
    data.put("metadata", METADATA);
    data.put("script", script);
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
    KubeUtils.printLogs(namespace, "designs");
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
    if (KubeUtils.buildDockerImage(".", "integration/designs:" + version, args) != 0) {
      fail("Can't build image");
    }
    System.out.println("Image created");
    buildDockerImages = false;
  }

  private static void installServices() throws IOException, InterruptedException {
    installService("designs");
  }

  private static void uninstallServices() throws IOException, InterruptedException {
    uninstallService("designs");
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

  private static void installMySQL() throws IOException, InterruptedException {
    System.out.println("Installing MySQL...");
    final List<String> args = Arrays.asList("--set=replicas=1");
    if (KubeUtils.installHelmChart(namespace, "integration-mysql", "../../helm/mysql", args, true) != 0) {
      if (KubeUtils.upgradeHelmChart(namespace, "integration-mysql", "../../helm/mysql", args, true) != 0) {
        fail("Can't install or upgrade Helm chart");
      }
    }
    System.out.println("MySQL installed");
  }

  private static void uninstallMySQL() throws IOException, InterruptedException {
    System.out.println("Uninstalling MySQL...");
    if (KubeUtils.uninstallHelmChart(namespace, "integration-mysql") != 0) {
      System.out.println("Can't uninstall Helm chart");
    }
    System.out.println("MySQL uninstalled");
  }

  private static void waitForMySQL() {
    awaitUntilCondition(60, 10, 5, () -> isMySQLReady(namespace));
  }

  private static boolean isMySQLReady(String namespace) throws IOException, InterruptedException {
    String logs = KubeUtils.fetchLogs(namespace, "mysql");
    String[] lines = logs.split("\n");
    boolean databaseReady = Arrays.stream(lines).anyMatch(line -> line.contains("/usr/local/bin/docker-entrypoint.sh: running /docker-entrypoint-initdb.d/init.sql"));
    boolean serverReady = Arrays.stream(lines).anyMatch(line -> line.contains("/usr/sbin/mysqld: ready for connections.") && line.contains("socket: '/var/run/mysqld/mysqld.sock'  port: 3306"));
    return serverReady && databaseReady;
  }

  private static void waitForService() {
    awaitUntilCondition(20, 5, 1, () -> isServiceReady(namespace, "designs"));
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
    if (KubeUtils.createSecret(namespace,"designs", args, true) != 0) {
      fail("Can't create secret");
    }
    System.out.println("Secrets created");
  }

  private static void exposeService() throws IOException, InterruptedException {
    System.out.println("Exposing service...");
    if (KubeUtils.exposeService(namespace,"designs", Integer.parseInt(httpPort), 8080) != 0) {
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
    await()
            .atMost(timeout, TimeUnit.SECONDS)
            .pollDelay(delay, TimeUnit.SECONDS)
            .pollInterval(interval, TimeUnit.SECONDS)
            .untilAsserted(assertion);
  }

  public static void awaitUntilCondition(int timeout, int delay, int interval, Callable<Boolean> condition) {
    await()
            .atMost(timeout, TimeUnit.SECONDS)
            .pollDelay(delay, TimeUnit.SECONDS)
            .pollInterval(interval, TimeUnit.SECONDS)
            .until(condition);
  }
}
