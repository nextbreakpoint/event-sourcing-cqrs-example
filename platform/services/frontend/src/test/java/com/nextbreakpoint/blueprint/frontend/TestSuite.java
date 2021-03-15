package com.nextbreakpoint.blueprint.frontend;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.jayway.jsonpath.JsonPath;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.Authority;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.Cookie;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag("slow")
public class TestSuite {
  private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
  private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";
  private static final UUID DESIGN_UUID_1 = new UUID(1L, 1L);
  private static final UUID DESIGN_UUID_2 = new UUID(1L, 2L);
  private static final UUID ACCOUNT_UUID = new UUID(1L, 1L);

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
  @Tag("integration")
  @DisplayName("Verify behaviour of frontend service")
  public class VerifyServiceIntegration {
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

  @Nested
  @Tag("pact")
  @DisplayName("Test frontend pact")
  @ExtendWith(PactConsumerTestExt.class)
  public class PactConsumerTest {
    @Pact(consumer = "frontend")
    public RequestResponsePact retrieveDesigns(PactDslWithProvider builder) {
      final Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      return builder
              .given("designs exist")
              .uponReceiving("request to retrieve designs")
              .method("GET")
              .path("/v1/designs")
              .matchHeader("Accept", "application/json")
              .matchHeader("Authorization", "Bearer .+")
              .willRespondWith()
              .headers(headers)
              .status(200)
              .body(
                      new PactDslJsonArray()
                              .object()
                              .stringValue("uuid", DESIGN_UUID_1.toString())
                              .stringMatcher("checksum", ".+")
                              .closeObject()
                              .object()
                              .stringValue("uuid", DESIGN_UUID_2.toString())
                              .stringMatcher("checksum", ".+")
                              .closeObject()
              )
              .toPact();
    }

    @Pact(consumer = "frontend")
    public RequestResponsePact retrieveDesign(PactDslWithProvider builder) {
      final Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      return builder
              .given("design exists for uuid")
              .uponReceiving("request to fetch design")
              .method("GET")
              .path("/v1/designs/" + DESIGN_UUID_1.toString())
              .matchHeader("Accept", "application/json")
              .matchHeader("Authorization", "Bearer .+")
              .willRespondWith()
              .headers(headers)
              .status(200)
              .body(
                      new PactDslJsonBody()
                              .stringValue("uuid", DESIGN_UUID_1.toString())
                              .stringMatcher("json", ".+")
                              .stringMatcher("checksum", ".+")
                              .timestamp("modified", "yyyy-MM-dd'T'HH:mm:ss'Z'")
              )
              .toPact();
    }

    @Pact(consumer = "frontend")
    public RequestResponsePact retrieveAccount(PactDslWithProvider builder) {
      final Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      return builder
              .given("account exists for uuid")
              .uponReceiving("request to fetch account")
              .method("GET")
              .path("/v1/accounts/" + ACCOUNT_UUID.toString())
              .matchHeader("Accept", "application/json")
              .matchHeader("Authorization", "Bearer .+")
              .willRespondWith()
              .headers(headers)
              .status(200)
              .body(
                      new PactDslJsonBody()
                              .stringValue("uuid", ACCOUNT_UUID.toString())
                              .stringValue("role", "guest")
              )
              .toPact();
    }

    @Test
    @PactTestFor(providerName = "designs", port = "1111", pactMethod = "retrieveDesigns")
    public void shouldRetrieveDesigns(MockServer mockServer) throws IOException {
      HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/designs")
              .addHeader("Accept", "application/json")
              .addHeader("Authorization", "Bearer abcdef")
              .execute().returnResponse();
      assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[0].uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[0].checksum").toString()).isNotBlank();
      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[1].uuid").toString()).isEqualTo(DESIGN_UUID_2.toString());
      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[1].checksum").toString()).isNotBlank();
    }

    @Test
    @PactTestFor(providerName = "designs", port = "2222", pactMethod = "retrieveDesign")
    public void shouldRetrieveDesign(MockServer mockServer) throws IOException {
      HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/designs/" + DESIGN_UUID_1.toString())
              .addHeader("Accept", "application/json")
              .addHeader("Authorization", "Bearer abcdef")
              .execute().returnResponse();
      assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.json").toString()).isNotBlank();
      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.checksum").toString()).isNotBlank();
      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.modified").toString()).isNotBlank();
    }

    @Test
    @PactTestFor(providerName = "accounts", port = "3333", pactMethod = "retrieveAccount")
    public void shouldRetrieveAccount(MockServer mockServer) throws IOException {
      HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/accounts/" + ACCOUNT_UUID.toString())
              .addHeader("Accept", "application/json")
              .addHeader("Authorization", "Bearer abcdef")
              .execute().returnResponse();
      assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isEqualTo(ACCOUNT_UUID.toString());
      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.role").toString()).isEqualTo("guest");
    }
  }
}
