package com.nextbreakpoint.blueprint.accounts;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Headers;
import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import org.apache.http.HttpRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag("slow")
public class TestSuite {
  private final UUID ACCOUNT_UUID = new UUID(1L, 1L);

  private static final TestScenario scenario = new TestScenario();

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    scenario.before();
    System.setProperty("pact.verifier.publishResults", "true");
    System.setProperty("pact.provider.version", scenario.getVersion());
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    scenario.after();
  }

  @Nested
  @Tag("integration")
  @DisplayName("Verify behaviour of accounts service")
  public class VerifyServiceIntegration {
    private final AtomicInteger counter = new AtomicInteger(10);

    @AfterEach
    public void reset() {
      RestAssured.reset();
    }

    @Test
    @DisplayName("should allow options request without access token")
    public void shouldAllowOptionsRequestWithoutAccessToken() throws MalformedURLException {
      given().config(scenario.getRestAssuredConfig())
              .with().header("Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .when().queryParam("email", "test@localhost")
              .when().options(scenario.makeBaseURL("/v1/accounts"))
              .then().assertThat().statusCode(204)
              .and().header("Access-Control-Allow-Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .and().header("Access-Control-Allow-Credentials", "true");

      given().config(scenario.getRestAssuredConfig())
              .with().header("Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .when().options(scenario.makeBaseURL("/v1/accounts/me"))
              .then().assertThat().statusCode(204)
              .and().header("Access-Control-Allow-Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .and().header("Access-Control-Allow-Credentials", "true");

      given().config(scenario.getRestAssuredConfig())
              .with().header("Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .when().options(scenario.makeBaseURL("/v1/accounts/" + UUID.randomUUID().toString()))
              .then().assertThat().statusCode(204)
              .and().header("Access-Control-Allow-Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .and().header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("should forbid get request without access token")
    public void shouldForbidGetRequestWithoutAccessToken() throws MalformedURLException {
      given().config(scenario.getRestAssuredConfig())
              .with().accept(ContentType.JSON)
              .when().queryParam("email", "test@localhost")
              .when().get(scenario.makeBaseURL("/v1/accounts"))
              .then().assertThat().statusCode(403);

      given().config(scenario.getRestAssuredConfig())
              .with().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/accounts/me"))
              .then().assertThat().statusCode(403);

      given().config(scenario.getRestAssuredConfig())
              .with().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/accounts/" + UUID.randomUUID().toString()))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("should forbid post request without access token")
    public void shouldForbidPostRequestWithoutAccessToken() throws MalformedURLException {
      given().config(scenario.getRestAssuredConfig())
              .and().contentType(ContentType.JSON)
              .and().accept(ContentType.JSON)
              .and().body(createPostData(makeUniqueEmail(), "guest"))
              .when().post(scenario.makeBaseURL("/v1/accounts"))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("should forbid delete request without access token")
    public void shouldForbidDeleteRequestWithoutAccessToken() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

      pause();

      final String uuid = createAccount(authorization, createPostData(makeUniqueEmail(), "guest"));

      given().config(scenario.getRestAssuredConfig())
              .and().accept(ContentType.JSON)
              .when().delete(scenario.makeBaseURL("/v1/accounts/" + uuid))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("should forbid get request when user doesn't have permissions")
    public void shouldForbidGetRequestWhenUserDoNotHavePermissions() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

      pause();

      final String uuid = createAccount(authorization, createPostData(makeUniqueEmail(), "guest"));

      final String otherAuthorization = scenario.makeAuthorization("test", "other");

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, otherAuthorization)
              .and().accept(ContentType.JSON)
              .when().queryParam("email", "test@localhost")
              .when().get(scenario.makeBaseURL("/v1/accounts"))
              .then().assertThat().statusCode(403);

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, otherAuthorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/accounts/" + uuid))
              .then().assertThat().statusCode(403);

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, otherAuthorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/accounts/me"))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("should allow get request when user has permissions")
    public void shouldAllowGetRequestWhenUserHasPlatformPermissions() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

      pause();

      final String uuid = createAccount(authorization, createPostData(makeUniqueEmail(), "guest"));

      final String adminAuthorization = scenario.makeAuthorization("test", Authority.ADMIN);

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, adminAuthorization)
              .and().accept(ContentType.JSON)
              .when().queryParam("email", "test@localhost")
              .when().get(scenario.makeBaseURL("/v1/accounts"))
              .then().assertThat().statusCode(200);

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, adminAuthorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/accounts/" + uuid))
              .then().assertThat().statusCode(200);

      final String guestAuthorization = scenario.makeAuthorization(uuid, Authority.GUEST);

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, guestAuthorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/accounts/me" ))
              .then().assertThat().statusCode(200);
    }

    @Test
    @DisplayName("should create and delete accounts")
    public void shouldCreateAndDeleteDesigns() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

      pause();

      final String email1 = "user1@localhost";
      final String email2 = "user2@localhost";

      final Map<String, Object> account1 = createPostData(email1, "guest");
      final Map<String, Object> account2 = createPostData(email2, "guest");

      final String uuid1 = createAccount(authorization, account1);

      pause();

      final JsonPath json1 = getAccount(authorization, uuid1);

      assertThat(json1.getString("uuid")).isEqualTo(uuid1);
      assertThat(json1.getString("role")).isEqualTo("guest");

      final String uuid2 = createAccount(authorization, account2);

      pause();

      final JsonPath json2 = getAccount(authorization, uuid2);

      assertThat(json2.getString("uuid")).isEqualTo(uuid2);
      assertThat(json2.getString("role")).isEqualTo("guest");

      assertThat(getAccounts(authorization)).contains(uuid1, uuid2);

      assertThat(findAccount(authorization, email1)).contains(uuid1);

      assertThat(findAccount(authorization, email2)).contains(uuid2);

      deleteAccount(authorization, uuid1);

      pause();

      assertThat(getAccounts(authorization)).contains(uuid2);
      assertThat(getAccounts(authorization)).doesNotContain(uuid1);

      deleteAccount(authorization, uuid2);

      pause();

      assertThat(getAccounts(authorization)).doesNotContain(uuid1, uuid2);
    }

    private void pause() {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ignored) {
      }
    }

    private void deleteAccount(String authorization, String uuid) throws MalformedURLException {
      given().config(scenario.getRestAssuredConfig())
              .and().header(AUTHORIZATION, authorization)
              .and().accept(ContentType.JSON)
              .when().delete(scenario.makeBaseURL("/v1/accounts/" + uuid))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.JSON)
              .and().body("uuid", notNullValue());
    }

    private String[] findAccount(String authorization, String email) throws MalformedURLException {
      return given().config(scenario.getRestAssuredConfig())
              .and().header(AUTHORIZATION, authorization)
              .and().accept(ContentType.JSON)
              .and().queryParam("email", email)
              .when().get(scenario.makeBaseURL("/v1/accounts"))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.JSON)
              .and().extract().as(String[].class);
    }

    private String[] getAccounts(String authorization) throws MalformedURLException {
      return given().config(scenario.getRestAssuredConfig())
              .and().header(AUTHORIZATION, authorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/accounts"))
              .then().assertThat().statusCode(200)
              .and().contentType(ContentType.JSON)
              .and().extract().body().as(String[].class);
    }

    private JsonPath getAccount(String authorization, String uuid) throws MalformedURLException {
      return given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, authorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/accounts/" + uuid))
              .then().assertThat().statusCode(200)
              .and().extract().jsonPath();
    }

    private String createAccount(String authorization, Map<String, Object> account) throws MalformedURLException {
      return given().config(scenario.getRestAssuredConfig())
              .and().header(AUTHORIZATION, authorization)
              .and().contentType(ContentType.JSON)
              .and().accept(ContentType.JSON)
              .and().body(account)
              .when().post(scenario.makeBaseURL("/v1/accounts"))
              .then().assertThat().statusCode(201)
              .and().contentType(ContentType.JSON)
              .and().body("uuid", notNullValue())
              .and().extract().response().body().jsonPath().getString("uuid");
    }

    private Map<String, Object> createPostData(String email, String role) {
      final Map<String, Object> data = new HashMap<>();
      data.put("email", email);
      data.put("name", "test");
      data.put("role", role);
      return data;
    }

    private String makeUniqueEmail() {
      return "user" + counter.getAndIncrement() + "@localhost";
    }
  }

  @Nested
  @Tag("pact")
  @DisplayName("Verify contract between accounts and authentication")
  @Provider("accounts")
  @Consumer("authentication")
  @PactBroker
  public class VerifyAuthenticationPact {
    @BeforeEach
    public void before(PactVerificationContext context) {
      context.setTarget(new HttpsTestTarget(scenario.getServiceHost(), Integer.parseInt(scenario.getServicePort()), "/", true));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    @DisplayName("Verify interaction")
    public void pactVerificationTestTemplate(PactVerificationContext context, HttpRequest request) {
      final String authorization = scenario.makeAuthorization(Authentication.NULL_USER_UUID, Authority.PLATFORM);
      request.setHeader(Headers.AUTHORIZATION, authorization);
      context.verifyInteraction();
    }

    @State("account exists for email")
    public void accountExistsForEmail() throws IOException, InterruptedException {
      scenario.executeMySQLCommand(scenario.getNamespace(), "accounts", "TRUNCATE ACCOUNTS;");
      scenario.executeMySQLCommand(scenario.getNamespace(), "accounts", "INSERT INTO ACCOUNTS (UUID,NAME,EMAIL,ROLE) VALUES ('" + ACCOUNT_UUID + "','test','test@localhost','guest');");
    }

    @State("account exists for uuid")
    public void accountExistsForUuid() throws IOException, InterruptedException {
      scenario.executeMySQLCommand(scenario.getNamespace(), "accounts", "TRUNCATE ACCOUNTS;");
      scenario.executeMySQLCommand(scenario.getNamespace(), "accounts", "INSERT INTO ACCOUNTS (UUID,NAME,EMAIL,ROLE) VALUES ('" + ACCOUNT_UUID + "','test','test@localhost','guest');");
    }

    @State("user is authenticated")
    public void userHasAdminPermission() throws IOException, InterruptedException {
      scenario.executeMySQLCommand(scenario.getNamespace(), "accounts", "TRUNCATE ACCOUNTS;");
    }
  }

  @Nested
  @Tag("pact")
  @DisplayName("Verify contract between accounts and frontend")
  @Provider("accounts")
  @Consumer("frontend")
  @PactBroker
  public class VerifyFrontendPact {
    @BeforeEach
    public void before(PactVerificationContext context) {
      context.setTarget(new HttpsTestTarget(scenario.getServiceHost(), Integer.parseInt(scenario.getServicePort()), "/", true));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    @DisplayName("Verify interaction")
    public void pactVerificationTestTemplate(PactVerificationContext context, HttpRequest request) {
      final String authorization = scenario.makeAuthorization(Authentication.NULL_USER_UUID, Authority.PLATFORM);
      request.setHeader(Headers.AUTHORIZATION, authorization);
      context.verifyInteraction();
    }

    @State("account exists for uuid")
    public void accountExistsForUuid() throws IOException, InterruptedException {
      scenario.executeMySQLCommand(scenario.getNamespace(), "accounts", "TRUNCATE ACCOUNTS;");
      scenario.executeMySQLCommand(scenario.getNamespace(), "accounts", "INSERT INTO ACCOUNTS (UUID,NAME,EMAIL,ROLE) VALUES ('" + ACCOUNT_UUID + "','test','test@localhost','guest');");
    }
  }
}
