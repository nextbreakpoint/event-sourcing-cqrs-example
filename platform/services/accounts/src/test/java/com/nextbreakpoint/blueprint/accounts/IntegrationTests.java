package com.nextbreakpoint.blueprint.accounts;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.test.KubeUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.*;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.CoreMatchers.notNullValue;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@Tag("integration")
@DisplayName("Accounts service")
public class IntegrationTests {
  private static final String KEYSTORE_AUTH_JCEKS_PATH = "../../secrets/keystore_auth.jceks";

  private static final String version = "1.0.0-1";
  private static final String namespace = "integration";
  private static final long timestamp = System.currentTimeMillis();

  private static final String githubUsername = TestUtils.getVariable("GITHUB_USERNAME");
  private static final String githubPassword = TestUtils.getVariable("GITHUB_PASSWORD");

  private static boolean buildDockerImages = TestUtils.getVariable("BUILD_IMAGES", "false").equals("true");

  private static String httpPort = TestUtils.getVariable("HTTP_PORT", System.getProperty("http.port", "8080"));

  private static String minikubeHost;

  private static RestAssuredConfig restAssuredConfig;

  private static AtomicInteger counter = new AtomicInteger(10);

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
    createSecrets();
    installServices();
    waitForService();
    exposeService();
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    RestAssured.reset();
    describeResources();
    printLogs();
    uninstallServices();
    uninstallMySQL();
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
            .when().queryParam("email", "test@localhost")
            .when().options(makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://" + minikubeHost + ":" + httpPort)
            .and().header("Access-Control-Allow-Credentials", "true");

    given().config(restAssuredConfig)
            .with().header("Origin", "https://" + minikubeHost + ":" + httpPort)
            .when().options(makeBaseURL("/v1/accounts/me"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://" + minikubeHost + ":" + httpPort)
            .and().header("Access-Control-Allow-Credentials", "true");

    given().config(restAssuredConfig)
            .with().header("Origin", "https://" + minikubeHost + ":" + httpPort)
            .when().options(makeBaseURL("/v1/accounts/" + UUID.randomUUID().toString()))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://" + minikubeHost + ":" + httpPort)
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("should forbid get request without access token")
  public void shouldForbidGetRequestWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .with().accept(ContentType.JSON)
            .when().queryParam("email", "test@localhost")
            .when().get(makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/accounts/me"))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/accounts/" + UUID.randomUUID().toString()))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid post request without access token")
  public void shouldForbidPostRequestWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(createPostData(makeUniqueEmail(), "guest"))
            .when().post(makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid delete request without access token")
  public void shouldForbidDeleteRequestWithoutAccessToken() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    final String uuid = createAccount(authorization, createPostData(makeUniqueEmail(), "guest"));

    given().config(restAssuredConfig)
            .and().accept(ContentType.JSON)
            .when().delete(makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid get request when user doesn't have permissions")
  public void shouldForbidGetRequestWhenUserDoNotHavePermissions() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    final String uuid = createAccount(authorization, createPostData(makeUniqueEmail(), "guest"));

    final String otherAuthorization = VertxUtils.makeAuthorization("test", Arrays.asList("other"), KEYSTORE_AUTH_JCEKS_PATH);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().queryParam("email", "test@localhost")
            .when().get(makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/accounts/me"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should allow get request when user has permissions")
  public void shouldAllowGetRequestWhenUserHasPlatformPermissions() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    final String uuid = createAccount(authorization, createPostData(makeUniqueEmail(), "guest"));

    final String adminAuthorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, adminAuthorization)
            .and().accept(ContentType.JSON)
            .when().queryParam("email", "test@localhost")
            .when().get(makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(200);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, adminAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(200);

    final String guestAuthorization = VertxUtils.makeAuthorization(uuid, Arrays.asList(Authority.GUEST), KEYSTORE_AUTH_JCEKS_PATH);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, guestAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/accounts/me" ))
            .then().assertThat().statusCode(200);
  }

  @Test
  @DisplayName("should create and delete accounts")
  public void shouldCreateAndDeleteDesigns() throws MalformedURLException {
    final String authorization = VertxUtils.makeAuthorization("test", Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    pause();

    final String email1 = "user1@localhost";
    final String email2 = "user2@localhost";

    final Map<String, String> account1 = createPostData(email1, "guest");
    final Map<String, String> account2 = createPostData(email2, "guest");

    final String uuid1 = createAccount(authorization, account1);

    pause();

    final JsonPath json1 = getAccount(authorization, uuid1);

    assertThat(json1.getString("uuid")).isEqualTo(uuid1);
    assertThat(json1.getString("role")).isEqualTo("guest");

    final String uuid2 = createAccount(authorization, account2);

    pause();

    final JsonPath json2 = getAccount(authorization, uuid2);

    assertThat(json2.getString("uuid")).isEqualTo(uuid2);
    assertThat(json2.getString("role")).isEqualTo("guest");

    assertThat(getAccounts(authorization)).contains(uuid1, uuid2);

    assertThat(findAccount(authorization, email1)).contains(uuid1);

    assertThat(findAccount(authorization, email2)).contains(uuid2);

    deleteAccount(authorization, uuid1);

    pause();

    assertThat(getAccounts(authorization)).contains(uuid2);
    assertThat(getAccounts(authorization)).doesNotContain(uuid1);

    deleteAccount(authorization, uuid2);

    pause();

    assertThat(getAccounts(authorization)).doesNotContain(uuid1, uuid2);
  }

  private static void pause() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
  }

  private static void deleteAccount(String authorization, String uuid) throws MalformedURLException {
    given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().delete(makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue());
  }

  private static String[] findAccount(String authorization, String email) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .and().queryParam("email", email)
            .when().get(makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().as(String[].class);
  }

  private static String[] getAccounts(String authorization) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().body().as(String[].class);
  }

  private static JsonPath getAccount(String authorization, String uuid) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(200)
            .and().extract().jsonPath();
  }

  private static String createAccount(String authorization, Map<String, String> account) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(account)
            .when().post(makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(201)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue())
            .and().extract().response().body().jsonPath().getString("uuid");
  }

  private static Map<String, String> createPostData(String email, String role) {
    final Map<String, String> data = new HashMap<>();
    data.put("email", email);
    data.put("name", "test");
    data.put("role", role);
    return data;
  }

  private static String makeUniqueEmail() {
    return "user" + counter.getAndIncrement() + "@localhost";
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
    KubeUtils.printLogs(namespace, "accounts");
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
    if (KubeUtils.buildDockerImage(".", "integration/accounts:" + version, args) != 0) {
      fail("Can't build image");
    }
    System.out.println("Image created");
    buildDockerImages = false;
  }

  private static void installServices() throws IOException, InterruptedException {
    installService("accounts");
  }

  private static void uninstallServices() throws IOException, InterruptedException {
    uninstallService("accounts");
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
    awaitUntilCondition(20, 5, 1, () -> isServiceReady(namespace, "accounts"));
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
    if (KubeUtils.createSecret(namespace,"accounts", args, true) != 0) {
        fail("Can't create secret");
    }
    System.out.println("Secrets created");
  }

  private static void exposeService() throws IOException, InterruptedException {
    System.out.println("Exposing service...");
    if (KubeUtils.exposeService(namespace,"accounts", Integer.parseInt(httpPort), 8080) != 0) {
      fail("Can't expose service");
    }
    System.out.println("Service exposed");
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
