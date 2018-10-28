package com.nextbreakpoint.shop.web;

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
import io.vertx.rxjava.ext.web.Cookie;
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
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@DisplayName("Web service")
public class WebIT {
  private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
  private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";

  private static RestAssuredConfig restAssuredConfig;

  private static StubServer stubServer;

  private URL makeBaseURL(String path) throws MalformedURLException {
    final Integer port = Integer.getInteger("http.port", 8080);
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
  @DisplayName("should return HTML when requesting designs content page without token")
  public void shouldReturnHTMLWhenRequestingDesignsContentPageWithoutToken() throws MalformedURLException {
    final UUID uuid = UUID.randomUUID();

    whenHttp(stubServer)
            .match(get("/designs"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + uuid + "\",\"checksum\":\"1\"}]"));

    whenHttp(stubServer)
            .match(get("/designs"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + uuid + "\",\"checksum\":\"1\"}]"));

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

    whenHttp(stubServer)
            .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));

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

    whenHttp(stubServer)
            .match(get("/designs"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + designUuid + "\",\"checksum\":\"1\"}]"));

    whenHttp(stubServer)
            .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

    final Cookie cookie = TestHelper.makeCookie(accountUuid.toString(), Arrays.asList(Authority.GUEST), "localhost");

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

    whenHttp(stubServer)
            .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));

    whenHttp(stubServer)
            .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

    final Cookie cookie = TestHelper.makeCookie(accountUuid.toString(), Arrays.asList(Authority.GUEST), "localhost");

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
    final Cookie cookie = TestHelper.makeCookie("test", Arrays.asList(Authority.GUEST), "localhost");

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

    final Cookie cookie = TestHelper.makeCookie("test", Arrays.asList(Authority.GUEST), "localhost");

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
}
