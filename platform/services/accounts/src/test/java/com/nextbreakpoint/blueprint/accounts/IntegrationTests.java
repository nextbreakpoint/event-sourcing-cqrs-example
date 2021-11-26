package com.nextbreakpoint.blueprint.accounts;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.nextbreakpoint.blueprint.common.core.Authority;
import org.junit.jupiter.api.*;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag("slow")
@Tag("integration")
@DisplayName("Verify behaviour of accounts service")
public class IntegrationTests {
  private static final AtomicInteger counter = new AtomicInteger(10);

  private static final TestScenario scenario = new TestScenario();

  @BeforeAll
  public static void before() {
    scenario.before();
  }

  @AfterAll
  public static void after() {
    scenario.after();
  }

  @BeforeEach
  public void setup() throws SQLException {
    try (Connection connection = DriverManager.getConnection(scenario.getMySqlConnectionUrl(TestConstants.DATABASE_NAME), TestConstants.DATABASE_USERNAME, TestConstants.DATABASE_PASSWORD)) {
      connection.prepareStatement("TRUNCATE ACCOUNT;").execute();
    }
  }

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

    final String email1 = "user1@localhost";
    final String email2 = "user2@localhost";

    final Map<String, Object> account1 = createPostData(email1, "guest");
    final Map<String, Object> account2 = createPostData(email2, "guest");

    final String uuid1 = createAccount(authorization, account1);

    final JsonPath json1 = getAccount(authorization, uuid1);

    assertThat(json1.getString("uuid")).isEqualTo(uuid1);
    assertThat(json1.getString("role")).isEqualTo("guest");

    final String uuid2 = createAccount(authorization, account2);

    final JsonPath json2 = getAccount(authorization, uuid2);

    assertThat(json2.getString("uuid")).isEqualTo(uuid2);
    assertThat(json2.getString("role")).isEqualTo("guest");

    assertThat(getAccounts(authorization)).contains(uuid1, uuid2);

    assertThat(findAccount(authorization, email1)).contains(uuid1);

    assertThat(findAccount(authorization, email2)).contains(uuid2);

    deleteAccount(authorization, uuid1);

    assertThat(getAccounts(authorization)).contains(uuid2);
    assertThat(getAccounts(authorization)).doesNotContain(uuid1);

    deleteAccount(authorization, uuid2);

    assertThat(getAccounts(authorization)).doesNotContain(uuid1, uuid2);
  }

  private static String[] findAccount(String authorization, String email) throws MalformedURLException {
    return given().config(scenario.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .and().queryParam("email", email)
            .when().get(scenario.makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().as(String[].class);
  }

  private static String[] getAccounts(String authorization) throws MalformedURLException {
    return given().config(scenario.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(scenario.makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().body().as(String[].class);
  }

  private static JsonPath getAccount(String authorization, String uuid) throws MalformedURLException {
    return given().config(scenario.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(scenario.makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(200)
            .and().extract().jsonPath();
  }

  private static String createAccount(String authorization, Map<String, Object> account) throws MalformedURLException {
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

  private static void deleteAccount(String authorization, String uuid) throws MalformedURLException {
    given().config(scenario.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().delete(scenario.makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue());
  }

  private static Map<String, Object> createPostData(String email, String role) {
    final Map<String, Object> data = new HashMap<>();
    data.put("email", email);
    data.put("name", "test");
    data.put("role", role);
    return data;
  }

  private static String makeUniqueEmail() {
    return "user" + counter.getAndIncrement() + "@localhost";
  }
}
