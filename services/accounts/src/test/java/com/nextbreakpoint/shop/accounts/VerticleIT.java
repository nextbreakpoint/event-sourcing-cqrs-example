package com.nextbreakpoint.shop.accounts;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.nextbreakpoint.shop.common.model.Authority;
import com.nextbreakpoint.shop.common.vertx.TestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@DisplayName("Accounts service")
public class VerticleIT {
  private static RestAssuredConfig restAssuredConfig;
  private static AtomicInteger counter = new AtomicInteger(10);

  private URL makeBaseURL(String path) throws MalformedURLException {
    final Integer port = Integer.getInteger("http.port", 3002);
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

  @Test
  @DisplayName("should allow options request without access token")
  public void shouldAllowOptionsRequestWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .with().header("Origin", "https://localhost:8080")
            .when().options(makeBaseURL("/a/accounts"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://localhost:8080")
            .and().header("Access-Control-Allow-Credentials", "true");

    given().config(restAssuredConfig)
            .with().header("Origin", "https://localhost:8080")
            .when().options(makeBaseURL("/a/accounts/me"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://localhost:8080")
            .and().header("Access-Control-Allow-Credentials", "true");

    given().config(restAssuredConfig)
            .with().header("Origin", "https://localhost:8080")
            .when().options(makeBaseURL("/a/accounts/" + UUID.randomUUID().toString()))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://localhost:8080")
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("should forbid get request without access token")
  public void shouldForbidGetRequestWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .with().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts"))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts/me"))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts/" + UUID.randomUUID().toString()))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid post request without access token")
  public void shouldForbidPostRequestWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(createPostData(makeUniqueEmail(), "guest"))
            .when().post(makeBaseURL("/a/accounts"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid delete request without access token")
  public void shouldForbidDeleteRequestWithoutAccessToken() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    final String uuid = createAccount(authorization, createPostData(makeUniqueEmail(), "guest"));

    given().config(restAssuredConfig)
            .and().accept(ContentType.JSON)
            .when().delete(makeBaseURL("/a/accounts/" + uuid))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid get request when user doesn't have permissions")
  public void shouldForbidGetRequestWhenUserDoNotHavePermissions() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    final String uuid = createAccount(authorization, createPostData(makeUniqueEmail(), "guest"));

    final String otherAuthorization = TestHelper.makeAuthorization("test", Arrays.asList("other"));

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts" ))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts/" + uuid))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts/me"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should allow get request when user has permissions")
  public void shouldAllowGetRequestWhenUserHasPlatformPermissions() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    final String uuid = createAccount(authorization, createPostData(makeUniqueEmail(), "guest"));

    final String adminAuthorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, adminAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts" ))
            .then().assertThat().statusCode(200);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, adminAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts/" + uuid))
            .then().assertThat().statusCode(200);

    final String guestAuthorization = TestHelper.makeAuthorization(uuid, Arrays.asList(Authority.GUEST));

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, guestAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts/me" ))
            .then().assertThat().statusCode(200);
  }

  @Test
  @DisplayName("should create and delete accounts")
  public void shouldCreateAndDeleteDesigns() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    final String email1 = "user1@localhost";
    final String email2 = "user2@localhost";

    final Map<String, String> account1 = createPostData(email1, "guest");
    final Map<String, String> account2 = createPostData(email2, "guest");

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

  private void pause() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
  }

  private void deleteAccount(String authorization, String uuid) throws MalformedURLException {
    given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().delete(makeBaseURL("/a/accounts/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue());
  }

  private String[] findAccount(String authorization, String email) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .and().queryParam("email", email)
            .when().get(makeBaseURL("/a/accounts"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().as(String[].class);
  }

  private String[] getAccounts(String authorization) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().body().as(String[].class);
  }

  private JsonPath getAccount(String authorization, String uuid) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/accounts/" + uuid))
            .then().assertThat().statusCode(200)
            .and().extract().jsonPath();
  }

  private String createAccount(String authorization, Map<String, String> account) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(account)
            .when().post(makeBaseURL("/a/accounts"))
            .then().assertThat().statusCode(201)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue())
            .and().extract().response().body().jsonPath().getString("uuid");
  }

  private Map<String, String> createPostData(String email, String role) {
    final Map<String, String> data = new HashMap<>();
    data.put("email", email);
    data.put("name", "test");
    data.put("role", role);
    return data;
  }

  private String makeUniqueEmail() {
    return "user" + counter.getAndIncrement() + "@localhost";
  }
}
