package com.nextbreakpoint.shop.gateway;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.shop.common.model.Authority;
import com.nextbreakpoint.shop.common.vertx.TestHelper;
import com.xebialabs.restito.server.StubServer;
import io.vertx.core.json.JsonObject;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static com.xebialabs.restito.semantics.Condition.withPostBody;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@DisplayName("Gateway service")
public class VerticleIT {
  private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
  private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";

  private static RestAssuredConfig restAssuredConfig;

  private static StubServer stubServer;

  private URL makeBaseURL(String path) throws MalformedURLException {
    final Integer port = Integer.getInteger("http.port", 4000);
    final String normPath = path.startsWith("/") ? path.substring(1) : path;
    return new URL("https://localhost:" + port + "/" + normPath);
  }

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

  @BeforeAll
  public static void configureStubServer() {
    stubServer = new StubServer(Integer.getInteger("stub.port", 9090)).run();
  }

  @AfterAll
  public static void unconfigureStubServer() {
    stubServer.stop();
  }

  @BeforeEach
  public void clearStubServer() {
    stubServer.clear();
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

    final String authorization = TestHelper.makeAuthorization(accountUuid.toString(), Arrays.asList(Authority.GUEST));

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

    final String authorization = TestHelper.makeAuthorization(accountUuid.toString(), Arrays.asList(Authority.GUEST));

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

    final String authorization = TestHelper.makeAuthorization(accountUuid.toString(), Arrays.asList(Authority.ADMIN));

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

    final String authorization = TestHelper.makeAuthorization(accountUuid.toString(), Arrays.asList(Authority.GUEST));

    final String location = given().config(restAssuredConfig)
            .with().header("authorization", authorization)
            .with().header("accept", "application/json")
            .when().get(makeBaseURL("/watch/designs/0/" + designUuid))
            .then().assertThat().statusCode(200)
            .and().header("location", startsWith("https://127.0.0.10"))
            .and().header("location", endsWith("/watch/designs/0/" + designUuid))
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
}
