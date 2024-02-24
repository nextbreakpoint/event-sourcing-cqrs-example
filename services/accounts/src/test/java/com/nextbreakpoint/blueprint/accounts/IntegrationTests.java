package com.nextbreakpoint.blueprint.accounts;

import com.nextbreakpoint.blueprint.common.core.Authority;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of accounts service")
public class IntegrationTests {
  private static final TestCases testCases = new TestCases();

  @BeforeAll
  public static void before() {
    testCases.before();
  }

  @AfterAll
  public static void after() {
    testCases.after();
  }

  @BeforeEach
  public void setup() throws SQLException {
    try (Connection connection = DriverManager.getConnection(testCases.getMySqlConnectionUrl(TestConstants.DATABASE_NAME), TestConstants.DATABASE_USERNAME, TestConstants.DATABASE_PASSWORD)) {
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
    given().config(TestUtils.getRestAssuredConfig())
            .with().header("Origin", testCases.getOriginUrl())
            .when().queryParam("login", "test-login")
            .when().options(testCases.makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", testCases.getOriginUrl())
            .and().header("Access-Control-Allow-Credentials", "true");

    given().config(TestUtils.getRestAssuredConfig())
            .with().header("Origin", testCases.getOriginUrl())
            .when().options(testCases.makeBaseURL("/v1/accounts/me"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", testCases.getOriginUrl())
            .and().header("Access-Control-Allow-Credentials", "true");

    given().config(TestUtils.getRestAssuredConfig())
            .with().header("Origin", testCases.getOriginUrl())
            .when().options(testCases.makeBaseURL("/v1/accounts/" + UUID.randomUUID()))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", testCases.getOriginUrl())
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("should forbid get request without access token")
  public void shouldForbidGetRequestWithoutAccessToken() throws MalformedURLException {
    given().config(TestUtils.getRestAssuredConfig())
            .with().accept(ContentType.JSON)
            .when().queryParam("login", "test-login")
            .when().get(testCases.makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(403);

    given().config(TestUtils.getRestAssuredConfig())
            .with().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/accounts/me"))
            .then().assertThat().statusCode(403);

    given().config(TestUtils.getRestAssuredConfig())
            .with().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/accounts/" + UUID.randomUUID()))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid post request without access token")
  public void shouldForbidPostRequestWithoutAccessToken() throws MalformedURLException {
    given().config(TestUtils.getRestAssuredConfig())
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(TestUtils.createPostData(testCases.makeUniqueEmail(), "guest"))
            .when().post(testCases.makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid delete request without access token")
  public void shouldForbidDeleteRequestWithoutAccessToken() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

    final String uuid = createAccount(authorization, TestUtils.createPostData(testCases.makeUniqueEmail(), "guest"));

    given().config(TestUtils.getRestAssuredConfig())
            .and().accept(ContentType.JSON)
            .when().delete(testCases.makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid get request when user doesn't have permissions")
  public void shouldForbidGetRequestWhenUserDoNotHavePermissions() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

    final String uuid = createAccount(authorization, TestUtils.createPostData(testCases.makeUniqueEmail(), "guest"));

    final String otherAuthorization = testCases.makeAuthorization("test", "other");

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().queryParam("login", "test-login")
            .when().get(testCases.makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(403);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(403);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/accounts/me"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should allow get request when user has permissions")
  public void shouldAllowGetRequestWhenUserHasPlatformPermissions() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

    final String uuid = createAccount(authorization, TestUtils.createPostData(testCases.makeUniqueEmail(), "guest"));

    final String adminAuthorization = testCases.makeAuthorization("test", Authority.ADMIN);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, adminAuthorization)
            .and().accept(ContentType.JSON)
            .when().queryParam("login", "test-login")
            .when().get(testCases.makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(200);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, adminAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(200);

    final String guestAuthorization = testCases.makeAuthorization(uuid, Authority.GUEST);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, guestAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/accounts/me" ))
            .then().assertThat().statusCode(200);
  }

  @Test
  @DisplayName("should create and delete accounts")
  public void shouldCreateAndDeleteDesigns() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

    final String login1 = "test-login-1";
    final String login2 = "test-login-2";

    final Map<String, Object> account1 = TestUtils.createPostData(login1, "guest");
    final Map<String, Object> account2 = TestUtils.createPostData(login2, "guest");

    final String uuid1 = createAccount(authorization, account1);

    final JsonPath json1 = getAccount(authorization, uuid1);

    assertThat(json1.getString("uuid")).isEqualTo(uuid1);
    assertThat(json1.getString("role")).isEqualTo("guest");

    final String uuid2 = createAccount(authorization, account2);

    final JsonPath json2 = getAccount(authorization, uuid2);

    assertThat(json2.getString("uuid")).isEqualTo(uuid2);
    assertThat(json2.getString("role")).isEqualTo("guest");

    assertThat(getAccounts(authorization)).contains(uuid1, uuid2);

    assertThat(findAccount(authorization, login1)).contains(uuid1);

    assertThat(findAccount(authorization, login2)).contains(uuid2);

    deleteAccount(authorization, uuid1);

    assertThat(getAccounts(authorization)).contains(uuid2);
    assertThat(getAccounts(authorization)).doesNotContain(uuid1);

    deleteAccount(authorization, uuid2);

    assertThat(getAccounts(authorization)).doesNotContain(uuid1, uuid2);
  }

  private static String[] findAccount(String authorization, String login) throws MalformedURLException {
    return given().config(TestUtils.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .and().queryParam("login", login)
            .when().get(testCases.makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().as(String[].class);
  }

  private static String[] getAccounts(String authorization) throws MalformedURLException {
    return given().config(TestUtils.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().body().as(String[].class);
  }

  private static JsonPath getAccount(String authorization, String uuid) throws MalformedURLException {
    return given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(200)
            .and().extract().jsonPath();
  }

  private static String createAccount(String authorization, Map<String, Object> account) throws MalformedURLException {
    return given().config(TestUtils.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(account)
            .when().post(testCases.makeBaseURL("/v1/accounts"))
            .then().assertThat().statusCode(201)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue())
            .and().extract().response().body().jsonPath().getString("uuid");
  }

  private static void deleteAccount(String authorization, String uuid) throws MalformedURLException {
    given().config(TestUtils.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().delete(testCases.makeBaseURL("/v1/accounts/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue());
  }
}
