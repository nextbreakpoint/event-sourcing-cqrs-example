package com.nextbreakpoint.shop.web;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.shop.common.Authority;
import com.nextbreakpoint.shop.common.TestHelper;
import com.xebialabs.restito.server.StubServer;
import io.vertx.rxjava.ext.web.Cookie;
import org.apache.http.annotation.NotThreadSafe;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.shop.common.TimeUtil.TIMESTAMP_PATTERN;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.header;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@DisplayName("Web service")
@NotThreadSafe
public class VerticleIT {
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
    restAssuredConfig = RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig);
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

    final Date date = new Date();

    whenHttp(stubServer)
            .match(get("/api/designs"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), header("X-Modified", "" + date.getTime()), contentType("application/json"), stringContent("[\"" + uuid + "\"]"));

    given().config(restAssuredConfig)
            .when().get(makeBaseURL("/content/designs"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting preview content page without token")
  public void shouldReturnHTMLWhenRequestingPreviewContentPageWithoutToken() throws MalformedURLException {
    final UUID uuid = UUID.randomUUID();

    final SimpleDateFormat df = new SimpleDateFormat(TIMESTAMP_PATTERN);

    final Date date = new Date();

    whenHttp(stubServer)
            .match(get("/api/designs/" + uuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), header("X-Modified", "" + date.getTime()), contentType("application/json"), stringContent("{\"uuid\":\"" + uuid + "\",\"created\":\"" + df.format(date) + "\",\"updated\":\"" + df.format(date) + "\"}"));

    given().config(restAssuredConfig)
            .when().get(makeBaseURL("/content/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting designs content page with token")
  public void shouldReturnHTMLWhenRequestingDesignsContentPageWithToken() throws MalformedURLException {
    final UUID designUuid = UUID.randomUUID();
    final UUID accountUuid = UUID.randomUUID();

    final Date date = new Date();

    whenHttp(stubServer)
            .match(get("/api/designs"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), header("X-Modified", "" + date.getTime()), contentType("application/json"), stringContent("[\"" + designUuid + "\"]"));

    whenHttp(stubServer)
            .match(get("/api/accounts/" + accountUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

    final Cookie cookie = TestHelper.makeCookie(accountUuid.toString(), Arrays.asList(Authority.GUEST), "localhost");

    given().config(restAssuredConfig)
            .with().cookie("token", cookie.getValue())
            .when().get(makeBaseURL("/content/designs"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting preview content page with token")
  public void shouldReturnHTMLWhenRequestingPreviewContentPageWithToken() throws MalformedURLException {
    final UUID designUuid = UUID.randomUUID();
    final UUID accountUuid = UUID.randomUUID();

    final SimpleDateFormat df = new SimpleDateFormat(TIMESTAMP_PATTERN);

    final Date date = new Date();

    whenHttp(stubServer)
            .match(get("/api/designs/" + designUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), header("X-Modified", "" + date.getTime()), contentType("application/json"), stringContent("{\"uuid\":\"" + designUuid + "\",\"created\":\"" + df.format(date) + "\",\"updated\":\"" + df.format(date) + "\"}"));

    whenHttp(stubServer)
            .match(get("/api/accounts/" + accountUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

    final Cookie cookie = TestHelper.makeCookie(accountUuid.toString(), Arrays.asList(Authority.GUEST), "localhost");

    given().config(restAssuredConfig)
            .with().cookie("token", cookie.getValue())
            .when().get(makeBaseURL("/content/designs/" + designUuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting designs admin page without token")
  public void shouldReturnHTMLWhenRequestingDesignsAdminPageWithoutToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .when().get(makeBaseURL("/admin/designs"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app-designs"));
  }

  @Test
  @DisplayName("should return HTML when requesting preview admin page without token")
  public void shouldReturnHTMLWhenRequestingPreviewAdminPageWithoutToken() throws MalformedURLException {
    final UUID uuid = UUID.randomUUID();

    given().config(restAssuredConfig)
            .when().get(makeBaseURL("/admin/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app-preview"));
  }

  @Test
  @DisplayName("should return HTML when requesting designs admin page with token")
  public void shouldReturnHTMLWhenRequestingDesignsAdminPageWithToken() throws MalformedURLException {
    final Cookie cookie = TestHelper.makeCookie("test", Arrays.asList(Authority.GUEST), "localhost");

    whenHttp(stubServer)
            .match(get("/api/accounts/test"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"test\"}"));

    given().config(restAssuredConfig)
            .with().cookie("token", cookie.getValue())
            .when().get(makeBaseURL("/admin/designs"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app-designs"));
  }

  @Test
  @DisplayName("should return HTML when requesting preview admin page with token")
  public void shouldReturnHTMLWhenRequestingPreviewAdminPageWithToken() throws MalformedURLException {
    final UUID uuid = UUID.randomUUID();

    final Cookie cookie = TestHelper.makeCookie("test", Arrays.asList(Authority.GUEST), "localhost");

    whenHttp(stubServer)
            .match(get("/api/accounts/test"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"test\"}"));

    given().config(restAssuredConfig)
            .with().cookie("token", cookie.getValue())
            .when().get(makeBaseURL("/admin/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app-preview"));
  }
}
