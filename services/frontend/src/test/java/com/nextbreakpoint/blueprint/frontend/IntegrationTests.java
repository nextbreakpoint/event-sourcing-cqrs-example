package com.nextbreakpoint.blueprint.frontend;

import com.nextbreakpoint.blueprint.common.core.Authority;
import com.xebialabs.restito.server.StubServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.Cookie;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.*;

import java.net.MalformedURLException;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.*;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@Disabled
@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of frontend service")
public class IntegrationTests {
  private static final StubServer apiStub = new StubServer(Integer.parseInt("39001"));

  private static final TestCases testCases = new TestCases();

  @BeforeAll
  public static void before() {
    testCases.before();

    if (apiStub != null) {
      apiStub.start();
    }
  }

  @AfterAll
  public static void after() {
    testCases.after();

    if (apiStub != null) {
      apiStub.stop();
    }
  }

  @AfterEach
  public void reset() {
    RestAssured.reset();

    if (apiStub != null) {
      apiStub.clear();
    }
  }

  @Test
  @DisplayName("should return HTML when requesting designs content page without token")
  public void shouldReturnHTMLWhenRequestingDesignsContentPageWithoutToken() throws MalformedURLException {
    final UUID uuid = UUID.randomUUID();

    whenHttp(apiStub)
            .match(get("/designs"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + uuid + "\",\"checksum\":\"1\"}]"));

    given().config(TestUtils.getRestAssuredConfig())
            .when().get(testCases.makeBaseURL("/content/designs.html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting preview content page without token")
  public void shouldReturnHTMLWhenRequestingPreviewContentPageWithoutToken() throws MalformedURLException {
    final UUID designUuid = UUID.randomUUID();

    final Date date = new Date();

    final String json = new JsonObject()
            .put("manifest", TestConstants.MANIFEST)
            .put("metadata", TestConstants.METADATA)
            .put("script", TestConstants.SCRIPT)
            .encode();

    final String content = new JsonObject()
            .put("uuid", designUuid.toString())
            .put("json", json)
            .put("checksum", "1")
            .put("modified", DateTimeFormatter.ISO_INSTANT.format(date.toInstant()))
            .encode();

    whenHttp(apiStub)
            .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));

    given().config(TestUtils.getRestAssuredConfig())
            .when().get(testCases.makeBaseURL("/content/designs/" + designUuid + ".html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting designs content page with token")
  public void shouldReturnHTMLWhenRequestingDesignsContentPageWithToken() throws MalformedURLException {
    final UUID designUuid = UUID.randomUUID();
    final UUID accountUuid = UUID.randomUUID();

    whenHttp(apiStub)
            .match(get("/designs"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + designUuid + "\",\"checksum\":\"1\"}]"));

    whenHttp(apiStub)
            .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

    final Cookie cookie = testCases.makeCookie(accountUuid.toString(), Authority.GUEST);

    given().config(TestUtils.getRestAssuredConfig())
            .with().cookie("token", cookie.getValue())
            .when().get(testCases.makeBaseURL("/content/designs.html"))
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
            .put("manifest", TestConstants.MANIFEST)
            .put("metadata", TestConstants.METADATA)
            .put("script", TestConstants.SCRIPT)
            .encode();

    final String content = new JsonObject()
            .put("uuid", designUuid.toString())
            .put("json", json)
            .put("checksum", "1")
            .put("modified", DateTimeFormatter.ISO_INSTANT.format(date.toInstant()))
            .encode();

    whenHttp(apiStub)
            .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));

    whenHttp(apiStub)
            .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

    final Cookie cookie = testCases.makeCookie(accountUuid.toString(), Authority.GUEST);

    given().config(TestUtils.getRestAssuredConfig())
            .with().cookie("token", cookie.getValue())
            .when().get(testCases.makeBaseURL("/content/designs/" + designUuid + ".html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML);
  }

  @Test
  @DisplayName("should return HTML when requesting designs admin page without token")
  public void shouldReturnHTMLWhenRequestingDesignsAdminPageWithoutToken() throws MalformedURLException {
    given().config(TestUtils.getRestAssuredConfig())
            .when().get(testCases.makeBaseURL("/admin/designs.html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app"));
  }

  @Test
  @DisplayName("should return HTML when requesting preview admin page without token")
  public void shouldReturnHTMLWhenRequestingPreviewAdminPageWithoutToken() throws MalformedURLException {
    final UUID uuid = UUID.randomUUID();

    given().config(TestUtils.getRestAssuredConfig())
            .when().get(testCases.makeBaseURL("/admin/designs/" + uuid + ".html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app"));
  }

  @Test
  @DisplayName("should return HTML when requesting designs admin page with token")
  public void shouldReturnHTMLWhenRequestingDesignsAdminPageWithToken() throws MalformedURLException {
    final Cookie cookie = testCases.makeCookie("test", Authority.GUEST);

    whenHttp(apiStub)
            .match(get("/accounts/test"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"test\"}"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().cookie("token", cookie.getValue())
            .when().get(testCases.makeBaseURL("/admin/designs.html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app"));
  }

  @Test
  @DisplayName("should return HTML when requesting preview admin page with token")
  public void shouldReturnHTMLWhenRequestingPreviewAdminPageWithToken() throws MalformedURLException {
    final UUID uuid = UUID.randomUUID();

    final Cookie cookie = testCases.makeCookie("test", Authority.GUEST);

    whenHttp(apiStub)
            .match(get("/accounts/test"), withHeader("accept", "application/json"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"test\"}"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().cookie("token", cookie.getValue())
            .when().get(testCases.makeBaseURL("/admin/designs/" + uuid + ".html"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.HTML)
            .and().body("html.body.div.@id", equalTo("app"));
  }
}
