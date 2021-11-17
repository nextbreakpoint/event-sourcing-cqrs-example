package com.nextbreakpoint.blueprint.authentication;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.jayway.restassured.RestAssured;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.*;
import static com.xebialabs.restito.semantics.Condition.*;
import static org.hamcrest.CoreMatchers.startsWith;

@Tag("slow")
@Tag("pact")
@DisplayName("Test authentication pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
  private static final String OAUTH_TOKEN_PATH = "/login/oauth/access_token";
  private static final String OAUTH_USER_PATH = "/user";
  private static final String OAUTH_USER_EMAILS_PATH = "/user/emails";
  private static final UUID ACCOUNT_UUID = new UUID(1L, 1L);

  private static final TestScenario scenario = new TestScenario(11111);

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    scenario.before();
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    scenario.after();
  }

  @BeforeEach
  public void reset() {
    RestAssured.reset();
    scenario.getStubServer2().clear();
  }

  @Pact(consumer = "authentication")
  public RequestResponsePact accountExists(PactDslWithProvider builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder
            .given("account exists for uuid")
            .uponReceiving("request to retrieve accounts")
            .method("GET")
            .path("/v1/accounts")
            .matchQuery("email", "test[@]localhost")
            .matchHeader("Accept", "application/json")
            .matchHeader("Authorization", "Bearer .+")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                    new PactDslJsonArray()
                            .stringValue(ACCOUNT_UUID.toString())
            )
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

  @Pact(consumer = "authentication")
  public RequestResponsePact accountDoesNotExist(PactDslWithProvider builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder
            .given("account doesn't exist")
            .uponReceiving("request to retrieve empty accounts")
            .method("GET")
            .path("/v1/accounts")
            .matchQuery("email", "test[@]localhost")
            .matchHeader("Accept", "application/json")
            .matchHeader("Authorization", "Bearer .+")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                    new PactDslJsonArray()
            )
            .uponReceiving("request to create account")
            .method("POST")
            .path("/v1/accounts")
            .matchHeader("Content-Type", "application/json")
            .matchHeader("Accept", "application/json")
            .matchHeader("Authorization", "Bearer .+")
            .body(
                    new PactDslJsonBody()
                            .stringValue("email", "test@localhost")
                            .stringValue("name", "test")
                            .stringValue("role", "guest")
            )
            .willRespondWith()
            .headers(headers)
            .status(201)
            .body(
                    new PactDslJsonBody()
                            .stringMatcher("uuid", ".+")
                            .stringValue("role", "guest")
            )
            .toPact();
  }

//    @Pact(consumer = "authentication")
//    public RequestResponsePact retrieveAccount(PactDslWithProvider builder) {
//      final Map<String, String> headers = new HashMap<>();
//      headers.put("Content-Type", "application/json");
//      return builder
//              .given("account exists for uuid")
//              .uponReceiving("request to fetch account")
//              .method("GET")
//              .path("/v1/accounts/" + ACCOUNT_UUID.toString())
//              .matchHeader("Accept", "application/json")
//              .matchHeader("Authorization", "Bearer .+")
//              .willRespondWith()
//              .headers(headers)
//              .status(200)
//              .body(
//                      new PactDslJsonBody()
//                              .stringValue("uuid", ACCOUNT_UUID.toString())
//                              .stringValue("role", "guest")
//              )
//              .toPact();
//    }
//
//    @Pact(consumer = "authentication")
//    public RequestResponsePact createAccount(PactDslWithProvider builder) {
//      final Map<String, String> headers = new HashMap<>();
//      headers.put("Content-Type", "application/json");
//      return builder
//              .given("user is authenticated")
//              .uponReceiving("request to create account")
//              .method("POST")
//              .path("/v1/accounts")
//              .matchHeader("Content-Type", "application/json")
//              .matchHeader("Accept", "application/json")
//              .matchHeader("Authorization", "Bearer .+")
//              .body(
//                      new PactDslJsonBody()
//                              .stringValue("email", "test@localhost")
//                              .stringValue("name", "test")
//                              .stringValue("role", "guest")
//              )
//              .willRespondWith()
//              .headers(headers)
//              .status(201)
//              .body(
//                      new PactDslJsonBody()
//                              .stringMatcher("uuid", ".+")
//                              .stringValue("role", "guest")
//              )
//              .toPact();
//    }

  @Test
  @PactTestFor(providerName = "accounts", hostInterface = "0.0.0.0", port = "11111", pactMethod = "accountDoesNotExist")
  public void shouldCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserDoNotHaveAnAccount(MockServer mockServer) throws IOException, InterruptedException {
    whenHttp(scenario.getStubServer2())
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(scenario.getStubServer2())
            .match(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("{\"name\":\"test\"}"));

    whenHttp(scenario.getStubServer2())
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    given().config(scenario.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(scenario.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/content/designs"));

    verifyHttp(scenario.getStubServer2()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"));

//        HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/accounts?email=test@localhost")
//                .addHeader("Accept", "application/json")
//                .addHeader("Authorization", "Bearer abcdef")
//                .execute().returnResponse();
//        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//        assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[0]").toString()).isEqualTo(ACCOUNT_UUID.toString());
  }

  @Test
  @PactTestFor(providerName = "accounts", hostInterface = "0.0.0.0", port = "11111", pactMethod = "accountExists")
  public void shouldNotCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserAlreadyHasAnAccount(MockServer mockServer) throws IOException, InterruptedException {
    whenHttp(scenario.getStubServer2())
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(scenario.getStubServer2())
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    given().config(scenario.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(scenario.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/content/designs"));

    verifyHttp(scenario.getStubServer2()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH));

//        HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/accounts?email=test@localhost")
//                .addHeader("Accept", "application/json")
//                .addHeader("Authorization", "Bearer abcdef")
//                .execute().returnResponse();
//        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//        assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[0]").toString()).isEqualTo(ACCOUNT_UUID.toString());
  }

//    @Test
//    @PactTestFor(providerName = "accounts", port = "1111", pactMethod = "findAccountsMatchingEmail")
//    public void shouldFindAccountsMatchingEmail(MockServer mockServer) throws IOException {
//      HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/accounts?email=test@localhost")
//              .addHeader("Accept", "application/json")
//              .addHeader("Authorization", "Bearer abcdef")
//              .execute().returnResponse();
//      assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[0]").toString()).isEqualTo(ACCOUNT_UUID.toString());
//    }
//
//    @Test
//    @PactTestFor(providerName = "accounts", port = "2222", pactMethod = "retrieveAccount")
//    public void shouldRetrieveAccount(MockServer mockServer) throws IOException {
//      HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/accounts/" + ACCOUNT_UUID.toString())
//              .addHeader("Accept", "application/json")
//              .addHeader("Authorization", "Bearer abcdef")
//              .execute().returnResponse();
//      assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isEqualTo(ACCOUNT_UUID.toString());
//      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.role").toString()).isEqualTo("guest");
//    }
//
//    @Test
//    @PactTestFor(providerName = "accounts", port = "3333", pactMethod = "createAccount")
//    public void shouldCreateAccount(MockServer mockServer) throws IOException {
//      StringEntity entity = new StringEntity("{\"email\":\"test@localhost\",\"name\":\"test\",\"role\":\"guest\"}");
//      entity.setContentType("application/json");
//      HttpResponse httpResponse = Request.Post(mockServer.getUrl() + "/v1/accounts")
//              .addHeader("Accept", "application/json")
//              .addHeader("Authorization", "Bearer abcdef")
//              .body(entity)
//              .execute().returnResponse();
//      assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(201);
//      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isNotBlank();
//      assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.role").toString()).isEqualTo("guest");
//    }
}
