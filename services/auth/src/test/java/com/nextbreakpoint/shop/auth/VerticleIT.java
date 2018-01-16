package com.nextbreakpoint.shop.auth;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.xebialabs.restito.server.StubServer;
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
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.parameter;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static com.xebialabs.restito.semantics.Condition.withPostBody;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@DisplayName("Auth service")
@NotThreadSafe
public class VerticleIT {
  private static final String OAUTH_TOKEN_PATH = "/login/oauth/access_token";
  private static final String OAUTH_USER_PATH = "/user";
  private static final String OAUTH_USER_EMAILS_PATH = "/user/emails";
  private static final String ACCOUNTS_PATH = "/api/accounts";
  private static final String SOME_UUID = new UUID(0, 1).toString();

  private static RestAssuredConfig restAssuredConfig;

  private static StubServer stubServer;

  private URL makeBaseURL(String path) throws MalformedURLException {
    final Integer port = Integer.getInteger("http.port", 3000);
    final String normPath = path.startsWith("/") ? path.substring(1) : path;
    return new URL("https://localhost:" + port + "/" + normPath);
  }

  @BeforeAll
  public static void configureRestAssured() {
    final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
    final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
    restAssuredConfig = RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig);
  }

  @AfterAll
  public static void unconfigureRestAssured() {
    RestAssured.reset();
  }

  @BeforeAll
  public static void configureStubServer() {
    stubServer = new StubServer(Integer.getInteger("stub.port", 9000)).run();
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
  @DisplayName("signin should redirect to login when user is not authenticated")
  public void signinShouldRedirectToLoginWhenUserIsNotAuthenticated() throws MalformedURLException {
    given().config(restAssuredConfig)
            .when().get(makeBaseURL("/auth/signin"))
            .then().assertThat().statusCode(302)
            .and().header("Location", containsString("/login/oauth/authorize?"));
  }

  @Test
  @DisplayName("callback should create an account and redirect to designs when authenticated user doesn't have an account")
  public void callbackShouldCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserDoNotHaveAnAccount() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("{\"name\":\"test\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[]"));

    whenHttp(stubServer)
            .match(post(ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"))
            .then(status(HttpStatus.CREATED_201), stringContent("{\"role\":\"guest\", \"uuid\":\"" + SOME_UUID + "\"}"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/content/designs"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(post(ACCOUNTS_PATH), withPostBody(), withHeader("authorization"));
  }

  @Test
  @DisplayName("callback should not create an account and redirect to designs when authenticated user already has an account")
  public void callbackShouldNotCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserAlreadyHasAnAccount() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[\"" + SOME_UUID + "\"]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH + "/" + SOME_UUID))
            .then(status(HttpStatus.OK_200), stringContent("{\"role\":\"guest\", \"uuid\":\"" + SOME_UUID + "\"}"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/content/designs"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(ACCOUNTS_PATH + "/" + SOME_UUID));
  }

  @Test
  @DisplayName("callback should redirect to error 500 when it can't retrieve oauth data")
  public void callbackShouldRedirectToError403WhenItCannotRetrieveOAuthData() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/500"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().never(get(OAUTH_USER_EMAILS_PATH));
  }

  @Test
  @DisplayName("callback should redirect to error 403 when it can't retrieve user data")
  public void callbackShouldRedirectToError403WhenItCannotRetrieveUserData() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/403"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().never(post(ACCOUNTS_PATH));
  }

  @Test
  @DisplayName("callback should redirect to error 403 when it can't retrieve user account")
  public void callbackShouldRedirectToError403WhenItCannotRetrieveUserAccount() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/403"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().never(post(ACCOUNTS_PATH));
  }

  @Test
  @DisplayName("callback should redirect to error 500 when it retrieves a malformed list of accounts")
  public void callbackShouldRedirectToError500WhenItRetrievesAMalformedListOfAcccounts() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("x"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/500"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().never(post(ACCOUNTS_PATH));
  }

  @Test
  @DisplayName("callback should redirect to error 403 when it can't create an account")
  public void callbackShouldRedirectToError403WhenItCannotCreateAnAccount() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("{\"name\":\"test\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[]"));

    whenHttp(stubServer)
            .match(post(ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/403"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(post(ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"));
  }

  @Test
  @DisplayName("callback should redirect to error 403 when it can't retrieve an account")
  public void callbackShouldRedirectToError403WhenItCannotRetrieveAnAccount() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"name\":\"test\"}]"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[\"" + SOME_UUID + "\"]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH + "/" + SOME_UUID))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/403"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(ACCOUNTS_PATH + "/" + SOME_UUID));
  }

  @Test
  @DisplayName("callback should redirect to error 403 when it retrieves an invalid account")
  public void callbackShouldRedirectToError403WhenItRetrievesAnInvalidAccount() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[\"" + SOME_UUID + "\"]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH + "/" + SOME_UUID))
            .then(status(HttpStatus.OK_200), stringContent("{\"uuid\":\"" + SOME_UUID + "\"}"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/403"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH + "/" + SOME_UUID))
            .then(status(HttpStatus.OK_200), stringContent("{\"role\":\"guest\"}"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/403"));

    verifyHttp(stubServer).times(2, post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(ACCOUNTS_PATH + "/" + SOME_UUID));
  }

  @Test
  @DisplayName("callback should redirect to error 500 when it retrieves a malformed account")
  public void callbackShouldRedirectToError500WhenItRetrievesAMalformedAccount() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[\"" + SOME_UUID + "\"]"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH + "/" + SOME_UUID))
            .then(status(HttpStatus.OK_200), stringContent("x"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/500"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(ACCOUNTS_PATH + "/" + SOME_UUID));
  }

  @Test
  @DisplayName("callback should redirect to error 500 when it retrieves malformed user data")
  public void callbackShouldRedirectToError500WhenItRetrievesMalformedUserData() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("x"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/500"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().never(post(ACCOUNTS_PATH));
  }

  @Test
  @DisplayName("callback should redirect to error 403 when it retrieves malformed oauth response")
  public void callbackShouldRedirectToError403WhenItRetrievesMalformedOAuthResponse() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{}"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/auth/signin/content/designs")
            .when().get(makeBaseURL("/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://localhost:8080/error/403"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().never(get(OAUTH_USER_EMAILS_PATH));
  }
}
