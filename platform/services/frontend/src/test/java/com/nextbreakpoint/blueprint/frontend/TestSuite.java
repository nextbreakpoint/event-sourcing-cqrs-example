package com.nextbreakpoint.blueprint.frontend;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.Authority;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.Cookie;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.*;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static org.hamcrest.CoreMatchers.equalTo;

public class TestSuite {
  private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
  private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";

  private static final TestScenario scenario = new TestScenario();

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    scenario.before();
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    scenario.after();
  }

  @Nested
  @Tag("slow")
  @Tag("integration")
  @DisplayName("Verify behaviour of frontend service")
  public class VerifyService {
    @AfterEach
    public void reset() {
      RestAssured.reset();
      scenario.getStubServer().clear();
    }

    @Test
    @DisplayName("should return HTML when requesting designs content page without token")
    public void shouldReturnHTMLWhenRequestingDesignsContentPageWithoutToken() throws MalformedURLException {
      final UUID uuid = UUID.randomUUID();

      whenHttp(scenario.getStubServer())
              .match(get("/designs"), withHeader("accept", "application/json"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + uuid + "\",\"checksum\":\"1\"}]"));

      given().config(scenario.getRestAssuredConfig())
              .when().get(scenario.makeBaseURL("/content/designs.html"))
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

      whenHttp(scenario.getStubServer())
              .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));

      given().config(scenario.getRestAssuredConfig())
              .when().get(scenario.makeBaseURL("/content/designs/" + designUuid + ".html"))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.HTML);
    }

    @Test
    @DisplayName("should return HTML when requesting designs content page with token")
    public void shouldReturnHTMLWhenRequestingDesignsContentPageWithToken() throws MalformedURLException {
      final UUID designUuid = UUID.randomUUID();
      final UUID accountUuid = UUID.randomUUID();

      whenHttp(scenario.getStubServer())
              .match(get("/designs"), withHeader("accept", "application/json"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("[{\"uuid\":\"" + designUuid + "\",\"checksum\":\"1\"}]"));

      whenHttp(scenario.getStubServer())
              .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

      final Cookie cookie = scenario.makeCookie(accountUuid.toString(), Authority.GUEST);

      given().config(scenario.getRestAssuredConfig())
              .with().cookie("token", cookie.getValue())
              .when().get(scenario.makeBaseURL("/content/designs.html"))
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

      whenHttp(scenario.getStubServer())
              .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));

      whenHttp(scenario.getStubServer())
              .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

      final Cookie cookie = scenario.makeCookie(accountUuid.toString(), Authority.GUEST);

      given().config(scenario.getRestAssuredConfig())
              .with().cookie("token", cookie.getValue())
              .when().get(scenario.makeBaseURL("/content/designs/" + designUuid + ".html"))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.HTML);
    }

    @Test
    @DisplayName("should return HTML when requesting designs admin page without token")
    public void shouldReturnHTMLWhenRequestingDesignsAdminPageWithoutToken() throws MalformedURLException {
      given().config(scenario.getRestAssuredConfig())
              .when().get(scenario.makeBaseURL("/admin/designs.html"))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.HTML)
              .and().body("html.body.div.@id", equalTo("app"));
    }

    @Test
    @DisplayName("should return HTML when requesting preview admin page without token")
    public void shouldReturnHTMLWhenRequestingPreviewAdminPageWithoutToken() throws MalformedURLException {
      final UUID uuid = UUID.randomUUID();

      given().config(scenario.getRestAssuredConfig())
              .when().get(scenario.makeBaseURL("/admin/designs/" + uuid + ".html"))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.HTML)
              .and().body("html.body.div.@id", equalTo("app"));
    }

    @Test
    @DisplayName("should return HTML when requesting designs admin page with token")
    public void shouldReturnHTMLWhenRequestingDesignsAdminPageWithToken() throws MalformedURLException {
      final Cookie cookie = scenario.makeCookie("test", Authority.GUEST);

      whenHttp(scenario.getStubServer())
              .match(get("/accounts/test"), withHeader("accept", "application/json"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"test\"}"));

      given().config(scenario.getRestAssuredConfig())
              .with().cookie("token", cookie.getValue())
              .when().get(scenario.makeBaseURL("/admin/designs.html"))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.HTML)
              .and().body("html.body.div.@id", equalTo("app"));
    }

    @Test
    @DisplayName("should return HTML when requesting preview admin page with token")
    public void shouldReturnHTMLWhenRequestingPreviewAdminPageWithToken() throws MalformedURLException {
      final UUID uuid = UUID.randomUUID();

      final Cookie cookie = scenario.makeCookie("test", Authority.GUEST);

      whenHttp(scenario.getStubServer())
              .match(get("/accounts/test"), withHeader("accept", "application/json"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"test\"}"));

      given().config(scenario.getRestAssuredConfig())
              .with().cookie("token", cookie.getValue())
              .when().get(scenario.makeBaseURL("/admin/designs/" + uuid + ".html"))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.HTML)
              .and().body("html.body.div.@id", equalTo("app"));
    }
  }

  public static Map<String, Object> createPostData(String manifest, String metadata, String script) {
    final Map<String, Object> data = new HashMap<>();
    data.put("manifest", manifest);
    data.put("metadata", metadata);
    data.put("script", script);
    return data;
  }
}
