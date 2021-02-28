package com.nextbreakpoint.blueprint.gateway;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.test.KubeUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;
import com.xebialabs.restito.server.StubServer;
import io.vertx.core.json.JsonObject;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.given;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static com.xebialabs.restito.semantics.Condition.withPostBody;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@Tag("integration")
@DisplayName("Gateway service")
public class IntegrationTests {
  private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
  private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";

  private static final String KEYSTORE_AUTH_JCEKS_PATH = "../../secrets/keystore_auth.jceks";

  private static final String version = "1.0.0";
  private static final String namespace = "integration";
  private static final long timestamp = System.currentTimeMillis();

  private static final String githubUsername = TestUtils.getVariable("GITHUB_USERNAME");
  private static final String githubPassword = TestUtils.getVariable("GITHUB_PASSWORD");

  private static boolean buildDockerImages = TestUtils.getVariable("BUILD_IMAGES", "false").equals("true");

  private static String httpPort = TestUtils.getVariable("HTTP_PORT", System.getProperty("http.port", "8080"));
  private static String stubPort = TestUtils.getVariable("STUB_PORT", System.getProperty("stub.port", "9000"));

  private static String minikubeHost;

  private static RestAssuredConfig restAssuredConfig;

  private static StubServer stubServer;

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    printInfo();
    configureMinikube();
    buildDockerImages();
    configureRestAssured();
    deleteNamespace();
    createNamespace();
    createSecrets();
    installConsul();
    waitForConsul();
    installServices();
    waitForService();
    exposeService();
    stubServer = new StubServer(Integer.parseInt(stubPort)).run();
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    if (stubServer != null) {
      stubServer.clear();
    }
    describeResources();
    printLogs();
    uninstallServices();
    uninstallConsul();
    deleteNamespace();
  }

  @BeforeEach
  public void setup() {
    if (stubServer != null) {
      stubServer.clear();
    }
  }

  @AfterEach
  public void reset() {
    RestAssured.reset();
  }

  @Test
  @DisplayName("should forward a GET request for a design")
  public void shouldForwardAGETRequestForADesign() throws MalformedURLException {
    final UUID designUuid = UUID.randomUUID();
    final UUID accountUuid = UUID.randomUUID();

    final Date date = new Date();

    final String json = new JsonObject()
            .put("manifest", MANIFEST)
            .put("metadata", METADATA)
            .put("script", SCRIPT)
            .encode();

    final String content = new JsonObject()
            .put("uuid", designUuid.toString())
            .put("json", json)
            .put("checksum", "1")
            .put("modified", DateTimeFormatter.ISO_INSTANT.format(date.toInstant()))
            .encode();

    whenHttp(stubServer)
            .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));

    final String authorization = VertxUtils.makeAuthorization(accountUuid.toString(), Arrays.asList(Authority.GUEST), KEYSTORE_AUTH_JCEKS_PATH);

    given().config(restAssuredConfig)
            .with().header("authorization", authorization)
            .with().header("accept", "application/json")
            .when().get(makeBaseURL("/designs/" + designUuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON);
  }

  // TODO add tests for other methods

  @Test
  @DisplayName("should forward a GET request for an account")
  public void shouldForwardAGETRequestForAnAccount() throws MalformedURLException {
    final UUID accountUuid = UUID.randomUUID();

    whenHttp(stubServer)
            .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

    final String authorization = VertxUtils.makeAuthorization(accountUuid.toString(), Arrays.asList(Authority.GUEST), KEYSTORE_AUTH_JCEKS_PATH);

    given().config(restAssuredConfig)
            .with().header("authorization", authorization)
            .with().header("accept", "application/json")
            .when().get(makeBaseURL("/accounts/" + accountUuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON);
  }

  @Test
  @DisplayName("should forward a POST request for an account")
  public void shouldForwardAPOSTRequestForAnAccount() throws MalformedURLException {
    final UUID accountUuid = UUID.randomUUID();

    whenHttp(stubServer)
            .match(post("/accounts"), withHeader("authorization"), withHeader("content-type"), withPostBody())
            .then(status(HttpStatus.CREATED_201), contentType("application/json"), stringContent("{\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

    final String authorization = VertxUtils.makeAuthorization(accountUuid.toString(), Arrays.asList(Authority.ADMIN), KEYSTORE_AUTH_JCEKS_PATH);

    final Map<String, String> account = createAccountData("user@localhost", "guest");

    given().config(restAssuredConfig)
            .with().header("authorization", authorization)
            .with().header("content-type", "application/json")
            .with().body(account)
            .when().post(makeBaseURL("/accounts"))
            .then().assertThat().statusCode(201)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", equalTo(accountUuid.toString()));
  }

  @Test
  @DisplayName("should return a Location when sending a watch request for a design")
  public void shouldReturnALocationWhenSendingAWatchRequestForADesign() throws MalformedURLException {
    final UUID designUuid = UUID.randomUUID();
    final UUID accountUuid = UUID.randomUUID();

    final String authorization = VertxUtils.makeAuthorization(accountUuid.toString(), Arrays.asList(Authority.GUEST), KEYSTORE_AUTH_JCEKS_PATH);

    final String location = given().config(restAssuredConfig)
            .with().header("authorization", authorization)
            .with().header("accept", "application/json")
            .when().get(makeBaseURL("/watch/designs/0/" + designUuid))
            .then().assertThat().statusCode(200)
            .and().header("location", startsWith("https://" + minikubeHost + ":" + httpPort))
            .and().header("location", endsWith("/sse/designs/0/" + designUuid))
            .extract().header("location");

    System.out.println(location);
  }

  private Map<String, String> createAccountData(String email, String role) {
    final Map<String, String> data = new HashMap<>();
    data.put("email", email);
    data.put("name", "test");
    data.put("role", role);
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
    KubeUtils.printLogs(namespace, "gateway");
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
    if (KubeUtils.buildDockerImage(".", "integration/gateway:" + version, args) != 0) {
      fail("Can't build image");
    }
    System.out.println("Image created");
    buildDockerImages = false;
  }

  private static void installServices() throws IOException, InterruptedException {
    installService("gateway");
  }

  private static void uninstallServices() throws IOException, InterruptedException {
    uninstallService("gateway");
  }

  private static void installService(String name) throws IOException, InterruptedException {
    System.out.println("Installing service...");
    final List<String> args = Arrays.asList("--set=replicas=1,clientDomain=" + minikubeHost + ",authApiUrl=http://192.168.64.1:" + stubPort + ",accountsApiUrl=http://192.168.64.1:" + stubPort + ",designsAggregateFetcherApiUrl=http://192.168.64.1:" + stubPort + ",designsCommandProducerApiUrl=http://192.168.64.1:" + stubPort + ",image.pullPolicy=Never,image.repository=integration/" + name + ",image.tag=" + version);
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
    awaitUntilCondition(20, 5, 1, () -> isServiceReady(namespace, "gateway"));
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
            "--from-file",
            "keystore_client.jks=../../secrets/keystore_client.jks",
            "--from-file",
            "truststore_client.jks=../../secrets/truststore_client.jks",
            "--from-literal",
            "KEYSTORE_SECRET=secret"
    );
    if (KubeUtils.createSecret(namespace,"gateway", args, true) != 0) {
      fail("Can't create secret");
    }
    System.out.println("Secrets created");
  }

  private static void exposeService() throws IOException, InterruptedException {
    System.out.println("Exposing service...");
    if (KubeUtils.exposeService(namespace,"gateway", Integer.parseInt(httpPort), 8080) != 0) {
      fail("Can't expose service");
    }
    System.out.println("Service exposed");
  }

  private static void installConsul() throws IOException, InterruptedException {
    System.out.println("Installing Consul...");
    final List<String> args = Arrays.asList("--set=replicas=1,serviceName=" + minikubeHost + ",servicePort=" + httpPort);
    if (KubeUtils.installHelmChart(namespace, "integration-consul", "../../helm/consul", args, true) != 0) {
      if (KubeUtils.upgradeHelmChart(namespace, "integration-consul", "../../helm/consul", args, true) != 0) {
        fail("Can't install or upgrade Helm chart");
      }
    }
    System.out.println("Consul installed");
  }

  private static void uninstallConsul() throws IOException, InterruptedException {
    System.out.println("Uninstalling Consul...");
    if (KubeUtils.uninstallHelmChart(namespace, "integration-consul") != 0) {
      System.out.println("Can't uninstall Helm chart");
    }
    System.out.println("Consul uninstalled");
  }

  private static void waitForConsul() {
    awaitUntilCondition(60, 10, 5, () -> isConsulReady(namespace));
  }

  private static boolean isConsulReady(String namespace) throws IOException, InterruptedException {
    String logs = KubeUtils.fetchLogs(namespace, "consul");
    String[] lines = logs.split("\n");
    boolean serverReady = Arrays.stream(lines).anyMatch(line -> line.contains("Synced service: service=designs-sse"));
    return serverReady;
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
