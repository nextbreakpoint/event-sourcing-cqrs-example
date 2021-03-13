package com.nextbreakpoint.blueprint.frontend;

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
import io.vertx.rxjava.core.http.Cookie;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.given;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@Tag("integration")
@DisplayName("Frontend service")
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
    installServices();
    waitForService();
    exposeService();
    stubServer = new StubServer(Integer.parseInt(stubPort)).run();
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    if (stubServer != null) {
      stubServer.stop();
    }
    describeResources();
    printLogs();
    uninstallServices();
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
  @DisplayName("should return HTML when requesting designs content page without token")
  public void shouldReturnHTMLWhenRequestingDesignsContentPageWithoutToken() throws MalformedURLException {
    final UUID uuid = UUID.randomUUID();

    whenHttp(stubServer)
            .match(get("/designs"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + uuid + "\",\"checksum\":\"1\"}]"));

//    whenHttp(stubServer)
//            .match(get("/designs"), withHeader("accept", "application/json"))
//            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + uuid + "\",\"checksum\":\"1\"}]"));

    given().config(restAssuredConfig)
            .when().get(makeBaseURL("/content/designs.html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting preview content page without token")
  public void shouldReturnHTMLWhenRequestingPreviewContentPageWithoutToken() throws MalformedURLException {
    final UUID designUuid = UUID.randomUUID();

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

//    whenHttp(stubServer)
//            .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
//            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));

    given().config(restAssuredConfig)
            .when().get(makeBaseURL("/content/designs/" + designUuid + ".html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting designs content page with token")
  public void shouldReturnHTMLWhenRequestingDesignsContentPageWithToken() throws MalformedURLException {
    final UUID designUuid = UUID.randomUUID();
    final UUID accountUuid = UUID.randomUUID();

    whenHttp(stubServer)
            .match(get("/designs"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + designUuid + "\",\"checksum\":\"1\"}]"));

//    whenHttp(stubServer)
//            .match(get("/designs"), withHeader("accept", "application/json"))
//            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + designUuid + "\",\"checksum\":\"1\"}]"));

    whenHttp(stubServer)
            .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

    final Cookie cookie = VertxUtils.makeCookie(accountUuid.toString(), Arrays.asList(Authority.GUEST), minikubeHost, KEYSTORE_AUTH_JCEKS_PATH);

    given().config(restAssuredConfig)
            .with().cookie("token", cookie.getValue())
            .when().get(makeBaseURL("/content/designs.html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting preview content page with token")
  public void shouldReturnHTMLWhenRequestingPreviewContentPageWithToken() throws MalformedURLException {
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

//    whenHttp(stubServer)
//            .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
//            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));

    whenHttp(stubServer)
            .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

    final Cookie cookie = VertxUtils.makeCookie(accountUuid.toString(), Arrays.asList(Authority.GUEST), minikubeHost, KEYSTORE_AUTH_JCEKS_PATH);

    given().config(restAssuredConfig)
            .with().cookie("token", cookie.getValue())
            .when().get(makeBaseURL("/content/designs/" + designUuid + ".html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting designs admin page without token")
  public void shouldReturnHTMLWhenRequestingDesignsAdminPageWithoutToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .when().get(makeBaseURL("/admin/designs.html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app"));
  }

  @Test
  @DisplayName("should return HTML when requesting preview admin page without token")
  public void shouldReturnHTMLWhenRequestingPreviewAdminPageWithoutToken() throws MalformedURLException {
    final UUID uuid = UUID.randomUUID();

    given().config(restAssuredConfig)
            .when().get(makeBaseURL("/admin/designs/" + uuid + ".html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app"));
  }

  @Test
  @DisplayName("should return HTML when requesting designs admin page with token")
  public void shouldReturnHTMLWhenRequestingDesignsAdminPageWithToken() throws MalformedURLException {
    final Cookie cookie = VertxUtils.makeCookie("test", Arrays.asList(Authority.GUEST), minikubeHost, KEYSTORE_AUTH_JCEKS_PATH);

    whenHttp(stubServer)
            .match(get("/accounts/test"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"test\"}"));

    given().config(restAssuredConfig)
            .with().cookie("token", cookie.getValue())
            .when().get(makeBaseURL("/admin/designs.html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app"));
  }

  @Test
  @DisplayName("should return HTML when requesting preview admin page with token")
  public void shouldReturnHTMLWhenRequestingPreviewAdminPageWithToken() throws MalformedURLException {
    final UUID uuid = UUID.randomUUID();

    final Cookie cookie = VertxUtils.makeCookie("test", Arrays.asList(Authority.GUEST), minikubeHost, KEYSTORE_AUTH_JCEKS_PATH);

    whenHttp(stubServer)
            .match(get("/accounts/test"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"test\"}"));

    given().config(restAssuredConfig)
            .with().cookie("token", cookie.getValue())
            .when().get(makeBaseURL("/admin/designs/" + uuid + ".html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app"));
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
    KubeUtils.printLogs(namespace, "frontend");
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
    if (KubeUtils.buildDockerImage(".", "integration/frontend:" + version, args) != 0) {
      fail("Can't build image");
    }
    System.out.println("Image created");
    buildDockerImages = false;
  }

  private static void installServices() throws IOException, InterruptedException {
    installService("frontend");
  }

  private static void uninstallServices() throws IOException, InterruptedException {
    uninstallService("frontend");
  }

  private static void installService(String name) throws IOException, InterruptedException {
    System.out.println("Installing service...");
    final List<String> args = Arrays.asList("--set=replicas=1,clientDomain=" + minikubeHost + ",clientWebUrl=https://" + minikubeHost + ":" + httpPort + ",clientAuthUrl=https://" + minikubeHost + ":" + httpPort + ",image.pullPolicy=Never,image.repository=integration/" + name + ",image.tag=" + version);
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
    awaitUntilCondition(20, 5, 1, () -> isServiceReady(namespace, "frontend"));
  }

  private static boolean isServiceReady(String namespace, String name) throws IOException, InterruptedException {
    String logs = KubeUtils.fetchLogs(namespace, name);
    String[] lines = logs.split("\n");
    boolean serviceReady = Arrays.stream(lines).anyMatch(line -> line.contains("Listening on port 8080"));
    return serviceReady;
  }

  private static void createSecrets() throws IOException, InterruptedException {
    System.out.println("Creating secrets...");
    final List<String> args = Arrays.asList(
            "--from-file",
            "ca_cert.pem=../../secrets/ca_cert.pem",
            "--from-file",
            "server_cert.pem=../../secrets/server_cert.pem",
            "--from-file",
            "server_key.pem=../../secrets/server_key.pem"
    );
    if (KubeUtils.createSecret(namespace,"frontend", args, true) != 0) {
      fail("Can't create secret");
    }
    System.out.println("Secrets created");
  }

  private static void exposeService() throws IOException, InterruptedException {
    System.out.println("Exposing service...");
    if (KubeUtils.exposeService(namespace,"frontend", Integer.parseInt(httpPort), 8080) != 0) {
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
