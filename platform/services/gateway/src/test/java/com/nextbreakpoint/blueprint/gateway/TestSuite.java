package com.nextbreakpoint.blueprint.gateway;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.Authority;
import io.vertx.core.json.JsonObject;
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
import static com.xebialabs.restito.semantics.Condition.*;
import static org.hamcrest.CoreMatchers.*;

@Tag("slow")
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
  @Tag("integration")
  @DisplayName("Verify behaviour of gateway service")
  public class VerifyServiceIntegration {
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

      whenHttp(scenario.getStubServer())
              .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));

      final String authorization = scenario.makeAuthorization(accountUuid.toString(), Authority.GUEST);

      given().config(scenario.getRestAssuredConfig())
              .with().header("authorization", authorization)
              .with().header("accept", "application/json")
              .when().get(scenario.makeBaseURL("/designs/" + designUuid))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.JSON);
    }

    // TODO add tests for other methods

    @Test
    @DisplayName("should forward a GET request for an account")
    public void shouldForwardAGETRequestForAnAccount() throws MalformedURLException {
      final UUID accountUuid = UUID.randomUUID();

      whenHttp(scenario.getStubServer())
              .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

      final String authorization = scenario.makeAuthorization(accountUuid.toString(), Authority.GUEST);

      given().config(scenario.getRestAssuredConfig())
              .with().header("authorization", authorization)
              .with().header("accept", "application/json")
              .when().get(scenario.makeBaseURL("/accounts/" + accountUuid))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("should forward a POST request for an account")
    public void shouldForwardAPOSTRequestForAnAccount() throws MalformedURLException {
      final UUID accountUuid = UUID.randomUUID();

      whenHttp(scenario.getStubServer())
              .match(post("/accounts"), withHeader("authorization"), withHeader("content-type"), withPostBody())
              .then(status(HttpStatus.CREATED_201), contentType("application/json"), stringContent("{\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));

      final String authorization = scenario.makeAuthorization(accountUuid.toString(), Authority.ADMIN);

      final Map<String, Object> account = createAccountData("user@localhost", "guest");

      given().config(scenario.getRestAssuredConfig())
              .with().header("authorization", authorization)
              .with().header("content-type", "application/json")
              .with().body(account)
              .when().post(scenario.makeBaseURL("/accounts"))
              .then().assertThat().statusCode(201)
              .and().contentType(ContentType.JSON)
              .and().body("uuid", equalTo(accountUuid.toString()));
    }

    @Test
    @DisplayName("should return a Location when sending a watch request for a design")
    public void shouldReturnALocationWhenSendingAWatchRequestForADesign() throws MalformedURLException {
      final UUID designUuid = UUID.randomUUID();
      final UUID accountUuid = UUID.randomUUID();

      final String authorization = scenario.makeAuthorization(accountUuid.toString(), Authority.GUEST);

      final String location = given().config(scenario.getRestAssuredConfig())
              .with().header("authorization", authorization)
              .with().header("accept", "application/json")
              .when().get(scenario.makeBaseURL("/watch/designs/0/" + designUuid))
              .then().assertThat().statusCode(200)
              .and().header("location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort()))
              .and().header("location", endsWith("/sse/designs/0/" + designUuid))
              .extract().header("location");

      System.out.println(location);
    }

    private Map<String, Object> createAccountData(String email, String role) {
      final Map<String, Object> data = new HashMap<>();
      data.put("email", email);
      data.put("name", "test");
      data.put("role", role);
      return data;
    }
  }
}
