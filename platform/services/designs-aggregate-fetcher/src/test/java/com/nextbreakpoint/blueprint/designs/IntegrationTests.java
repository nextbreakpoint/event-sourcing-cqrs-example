package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.DesignDocument;
import com.nextbreakpoint.blueprint.common.test.KubeUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.*;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@Tag("integration")
@DisplayName("Verify contract of service Designs Aggregate Fetcher")
public class IntegrationTests {
  private static final String JSON_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";

  private static final String UUID_0 = new UUID(0, 0).toString();
  private static final String UUID_1 = "48f6563e-e095-4d91-b473-4d8afc758c43";
  private static final String UUID_2 = "2fe33eed-58f7-425b-b10c-2e490c0f248e";
  private static final String UUID_3 = "51a0ce28-10a7-4a2e-ac6b-b80f682c0bb7";
  private static final String UUID_4 = "afb0f952-b26a-46de-8e65-40d5e98dc243";

  private static final String KEYSTORE_AUTH_JCEKS_PATH = "../../secrets/keystore_auth.jceks";

  private static final String version = "1.0.0";
  private static final String namespace = "integration";
  private static final long timestamp = System.currentTimeMillis();

  private static final String githubUsername = TestUtils.getVariable("GITHUB_USERNAME");
  private static final String githubPassword = TestUtils.getVariable("GITHUB_PASSWORD");

  private static boolean buildDockerImages = TestUtils.getVariable("BUILD_IMAGES", "false").equals("true");

  private static String httpPort = TestUtils.getVariable("HTTP_PORT", System.getProperty("http.port", "8080"));
  private static String cassandraPort = TestUtils.getVariable("CASSANDRA_PORT", System.getProperty("cassandra.port", "9042"));

  private static String minikubeHost;

  private static RestAssuredConfig restAssuredConfig;

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    printInfo();
    configureMinikube();
    buildDockerImages();
    configureRestAssured();
    deleteNamespace();
    createNamespace();
    installCassandra();
    waitForCassandra();
    exposeCassandra();
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
  @DisplayName("Should allow OPTIONS on /designs without access token")
  public void shouldAllowOptionsOnDesignsWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .with().header("Origin", "https://" + minikubeHost + ":" + httpPort)
            .when().options(makeBaseURL("/designs"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://" + minikubeHost + ":" + httpPort)
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("Should allow OPTIONS on /designs/id without access token")
  public void shouldAllowOptionsOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .with().header("Origin", "https://" + minikubeHost + ":" + httpPort)
            .when().options(makeBaseURL("/designs/" + UUID.randomUUID().toString()))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://" + minikubeHost + ":" + httpPort)
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("Should forbid GET on /designs when user has unknown authority")
  public void shouldForbidGetOnDesignsWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = VertxUtils.makeAuthorization("test", Arrays.asList("other"), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/designs"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should forbid GET on /designs/id when user has unknown authority")
  public void shouldForbidGetOnDesignsSlashIdWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = VertxUtils.makeAuthorization("test", Arrays.asList("other"), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/designs/" + UUID_1))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should forbid GET on /designs/id/zoom/x/y/256.png when user has unknown authority")
  public void shouldForbidGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = VertxUtils.makeAuthorization("test", Arrays.asList("other"), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/designs/" + UUID_2 + "/0/0/0/256.png"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should allow GET on /designs when user is anonymous")
  public void shouldAllowGetOnDesignsWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    DesignDocument[] results = listDesigns(authorization);

    assertThat(results[0].getUuid()).isEqualTo(UUID_1);
    assertThat(results[1].getUuid()).isEqualTo(UUID_2);
    assertThat(results[2].getUuid()).isEqualTo(UUID_3);
    assertThat(results[0].getChecksum()).isEqualTo("0");
    assertThat(results[1].getChecksum()).isEqualTo("4");
    assertThat(results[2].getChecksum()).isEqualTo("3");
    assertThat(results[0].getModified()).isNotNull();
    assertThat(results[1].getModified()).isNotNull();
    assertThat(results[2].getModified()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /designs/id when user is anonymous")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    JsonPath result = loadDesign(authorization, UUID_1);

    assertThat(result.getString("uuid")).isEqualTo(UUID_1);
    assertThat(result.getString("json")).isEqualTo(JSON_1);
    assertThat(result.getString("modified")).isNotNull();
    assertThat(result.getString("checksum")).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /designs/id/zoom/x/y/256.png when user is anonymous")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAnonymous() throws IOException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    byte[] result = getTile(authorization, UUID_2);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should allow GET on /designs when user is admin")
  public void shouldAllowGetOnDesignsWhenUserIsAdmin() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    DesignDocument[] results = listDesigns(authorization);

    assertThat(results[0].getUuid()).isEqualTo(UUID_1);
    assertThat(results[1].getUuid()).isEqualTo(UUID_2);
    assertThat(results[2].getUuid()).isEqualTo(UUID_3);
    assertThat(results[0].getChecksum()).isEqualTo("0");
    assertThat(results[1].getChecksum()).isEqualTo("4");
    assertThat(results[2].getChecksum()).isEqualTo("3");
    assertThat(results[0].getModified()).isNotNull();
    assertThat(results[1].getModified()).isNotNull();
    assertThat(results[2].getModified()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /designs/id when user is admin")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsAdmin() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    JsonPath result = loadDesign(authorization, UUID_1);

    assertThat(result.getString("uuid")).isEqualTo(UUID_1);
    assertThat(result.getString("json")).isEqualTo(JSON_1);
    assertThat(result.getString("modified")).isNotNull();
    assertThat(result.getString("checksum")).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /designs/id/zoom/x/y/256.png when user is admin")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAdmin() throws IOException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    byte[] result = getTile(authorization, UUID_2);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should allow GET on /designs when user is guest")
  public void shouldAllowGetOnDesignsWhenUserIsGuest() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.GUEST), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    DesignDocument[] results = listDesigns(authorization);

    assertThat(results[0].getUuid()).isEqualTo(UUID_1);
    assertThat(results[1].getUuid()).isEqualTo(UUID_2);
    assertThat(results[2].getUuid()).isEqualTo(UUID_3);
    assertThat(results[0].getChecksum()).isEqualTo("0");
    assertThat(results[1].getChecksum()).isEqualTo("4");
    assertThat(results[2].getChecksum()).isEqualTo("3");
    assertThat(results[0].getModified()).isNotNull();
    assertThat(results[1].getModified()).isNotNull();
    assertThat(results[2].getModified()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /designs/id when user is guest")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsGuest() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.GUEST), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    JsonPath result = loadDesign(authorization, UUID_1);

    assertThat(result.getString("uuid")).isEqualTo(UUID_1);
    assertThat(result.getString("json")).isEqualTo(JSON_1);
    assertThat(result.getString("modified")).isNotNull();
    assertThat(result.getString("checksum")).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /designs/id/zoom/x/y/256.png when user is guest")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsGuest() throws IOException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.GUEST), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    byte[] result = getTile(authorization, UUID_2);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /designs/id does not exist")
  public void shouldReturnNotFoundWhenDeisgnDoesNotExist() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/designs/" + UUID_0))
            .then().assertThat().statusCode(404);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /designs/id/zoom/x/y/256.png does not exist")
  public void shouldReturnNotFoundWhenDesignSlashLocationSlashSizeDoesNotExist() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/designs/" + UUID_0 + "/0/0/0/256.png"))
            .then().assertThat().statusCode(404);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /designs/id has been deleted")
  public void shouldReturnNotFoundWhenDeisgnHasBeenDeleted() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/designs/" + UUID_4))
            .then().assertThat().statusCode(404);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /designs/id/zoom/x/y/256.png  has been deleted")
  public void shouldReturnNotFoundWhenDesignSlashLocationSlashSizeHasBeenDeleted() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/designs/" + UUID_4 + "/0/0/0/256.png"))
            .then().assertThat().statusCode(404);
  }

  private void pause() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
    }
  }

  protected DesignDocument[] listDesigns(String authorization) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/designs"))
            .then().assertThat().statusCode(200)
            .extract().body().as(DesignDocument[].class);
  }

  protected JsonPath loadDesign(String authorization, String uuid) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .extract().body().jsonPath();
  }

  protected byte[] getTile(String authorization, String uuid) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(200)
            .and().assertThat().contentType("image/png")
            .extract().response().asByteArray();
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
    KubeUtils.printLogs(namespace, "designs-aggregate-fetcher");
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
    if (KubeUtils.buildDockerImage(".", "integration/designs-aggregate-fetcher:" + version, args) != 0) {
      fail("Can't build image");
    }
    System.out.println("Image created");
    buildDockerImages = false;
  }

  private static void installServices() throws IOException, InterruptedException {
    installService("designs-aggregate-fetcher");
  }

  private static void uninstallServices() throws IOException, InterruptedException {
    uninstallService("designs-aggregate-fetcher");
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
    awaitUntilCondition(20, 5, 1, () -> isServiceReady(namespace, "designs-aggregate-fetcher"));
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
            "KEYSTORE_SECRET=secret",
            "--from-literal",
            "DATABASE_USERNAME=verticle",
            "--from-literal",
            "DATABASE_PASSWORD=password"
    );
    if (KubeUtils.createSecret(namespace,"designs-aggregate-fetcher", args, true) != 0) {
      fail("Can't create secret");
    }
    System.out.println("Secrets created");
  }

  private static void exposeService() throws IOException, InterruptedException {
    System.out.println("Exposing service...");
    if (KubeUtils.exposeService(namespace,"designs-aggregate-fetcher", Integer.parseInt(httpPort), 8080) != 0) {
      fail("Can't expose service");
    }
    System.out.println("Service exposed");
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
    if (exposeService(namespace,"cassandra", Integer.parseInt(cassandraPort), 9042) != 0) {
      fail("Can't expose Cassandra");
    }
    System.out.println("Cassandra exposed");
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
