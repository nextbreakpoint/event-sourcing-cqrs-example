package com.nextbreakpoint.blueprint.authentication;

import com.xebialabs.restito.server.StubServer;
import io.restassured.RestAssured;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.*;

import java.net.MalformedURLException;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.*;
import static com.xebialabs.restito.semantics.Condition.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of authentication service")
public class IntegrationTests {
  private static final TestCases testCases = new TestCases();

  private static final StubServer accountsStub = new StubServer(Integer.parseInt("49001"));

  private static final StubServer githubStub = new StubServer(Integer.parseInt("49002"));

  private final int expectedPort = 8000;
  private final String expectedHost = "localhost";
  private final String expectedProtocol = "http";

  @BeforeAll
  public static void before() {
    testCases.before();

    if (accountsStub != null) {
      accountsStub.start();
    }

    if (githubStub != null) {
      githubStub.start();
    }
  }

  @AfterAll
  public static void after() {
    testCases.after();

    if (accountsStub != null) {
      accountsStub.stop();
    }

    if (githubStub != null) {
      githubStub.stop();
    }
  }

  @BeforeEach
  public void reset() {
    RestAssured.reset();


    if (accountsStub != null) {
      accountsStub.clear();
    }

    if (githubStub != null) {
      githubStub.clear();
    }
  }

  @Test
  @DisplayName("signin should redirect to login when user is not authenticated")
  public void signinShouldRedirectToLoginWhenUserIsNotAuthenticated() throws MalformedURLException {
    given().config(TestUtils.getRestAssuredConfig())
            .when().get(testCases.makeBaseURL("/v1/auth/signin/content/designs"))
            .then().assertThat().statusCode(302)
            .and().header("Location", containsString("/login/oauth/authorize?"));
  }

  @Test
  @DisplayName("should create an account and redirect to designs when authenticated user doesn't have an account")
  public void shouldCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserDoNotHaveAnAccount() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("{\"name\":\"test\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[]"));

    whenHttp(accountsStub)
            .match(post(TestConstants.ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"))
            .then(status(HttpStatus.CREATED_201), stringContent("{\"role\":\"guest\", \"uuid\":\"" + TestConstants.ACCOUNT_UUID + "\"}"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/content/designs"));

    verifyHttp(accountsStub)
            .once(post(TestConstants.ACCOUNTS_PATH), withPostBody(), withHeader("authorization"));
//          .once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(get(TestConstants.OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"));
  }

  @Test
  @DisplayName("should not create an account and redirect to designs when authenticated user already has an account")
  public void shouldNotCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserAlreadyHasAnAccount() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[\"" + TestConstants.ACCOUNT_UUID + "\"]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH + "/" + TestConstants.ACCOUNT_UUID))
            .then(status(HttpStatus.OK_200), stringContent("{\"role\":\"guest\", \"uuid\":\"" + TestConstants.ACCOUNT_UUID + "\"}"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/content/designs"));

    verifyHttp(accountsStub)
            .once(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(TestConstants.ACCOUNTS_PATH + "/" + TestConstants.ACCOUNT_UUID));

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(TestConstants.OAUTH_USER_PATH));
  }

  @Test
  @DisplayName("should redirect to error 500 when it can't retrieve oauth data")
  public void shouldRedirectToError403WhenItCannotRetrieveOAuthData() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/500"));

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().never(get(TestConstants.OAUTH_USER_EMAILS_PATH));
  }

  @Test
  @DisplayName("should redirect to error 403 when it can't retrieve user data")
  public void shouldRedirectToError403WhenItCannotRetrieveUserData() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/403"));

    verifyHttp(accountsStub)
            .never(post(TestConstants.ACCOUNTS_PATH));

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(TestConstants.OAUTH_USER_PATH));
  }

  @Test
  @DisplayName("should redirect to error 403 when it can't retrieve user account")
  public void shouldRedirectToError403WhenItCannotRetrieveUserAccount() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/403"));

    verifyHttp(accountsStub)
            .once(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().never(post(TestConstants.ACCOUNTS_PATH));

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(TestConstants.OAUTH_USER_PATH));
  }

  @Test
  @DisplayName("should redirect to error 500 when it retrieves a malformed list of accounts")
  public void shouldRedirectToError500WhenItRetrievesAMalformedListOfAcccounts() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("x"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/500"));

    verifyHttp(accountsStub)
            .once(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().never(post(TestConstants.ACCOUNTS_PATH));

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(TestConstants.OAUTH_USER_PATH));
  }

  @Test
  @DisplayName("should redirect to error 403 when it can't create an account")
  public void shouldRedirectToError403WhenItCannotCreateAnAccount() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("{\"name\":\"test\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[]"));

    whenHttp(accountsStub)
            .match(post(TestConstants.ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/403"));

    verifyHttp(accountsStub)
            .once(post(TestConstants.ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"));

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(get(TestConstants.OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"));
  }

  @Test
  @DisplayName("should redirect to error 403 when it can't retrieve an account")
  public void shouldRedirectToError403WhenItCannotRetrieveAnAccount() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"name\":\"test\"}]"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[\"" + TestConstants.ACCOUNT_UUID + "\"]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH + "/" + TestConstants.ACCOUNT_UUID))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/403"));

    verifyHttp(accountsStub)
            .once(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(TestConstants.ACCOUNTS_PATH + "/" + TestConstants.ACCOUNT_UUID));

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(TestConstants.OAUTH_USER_PATH));
  }

  @Test
  @DisplayName("should redirect to error 403 when it retrieves an invalid account")
  public void shouldRedirectToError403WhenItRetrievesAnInvalidAccount() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[\"" + TestConstants.ACCOUNT_UUID + "\"]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH + "/" + TestConstants.ACCOUNT_UUID))
            .then(status(HttpStatus.OK_200), stringContent("{\"uuid\":\"" + TestConstants.ACCOUNT_UUID + "\"}"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/403"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH + "/" + TestConstants.ACCOUNT_UUID))
            .then(status(HttpStatus.OK_200), stringContent("{\"role\":\"guest\"}"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/403"));

    verifyHttp(accountsStub)
            .times(2, get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(TestConstants.ACCOUNTS_PATH + "/" + TestConstants.ACCOUNT_UUID));

    verifyHttp(githubStub)
            .times(2, post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(TestConstants.OAUTH_USER_PATH));
  }

  @Test
  @DisplayName("should redirect to error 500 when it retrieves a malformed account")
  public void shouldRedirectToError500WhenItRetrievesAMalformedAccount() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then(status(HttpStatus.OK_200), stringContent("[\"" + TestConstants.ACCOUNT_UUID + "\"]"));

    whenHttp(accountsStub)
            .match(get(TestConstants.ACCOUNTS_PATH + "/" + TestConstants.ACCOUNT_UUID))
            .then(status(HttpStatus.OK_200), stringContent("x"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/500"));

    verifyHttp(accountsStub)
            .once(get(TestConstants.ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(TestConstants.ACCOUNTS_PATH + "/" + TestConstants.ACCOUNT_UUID));

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(TestConstants.OAUTH_USER_PATH));
  }

  @Test
  @DisplayName("should redirect to error 500 when it retrieves malformed user data")
  public void shouldRedirectToError500WhenItRetrievesMalformedUserData() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("x"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/500"));

    verifyHttp(accountsStub)
            .never(post(TestConstants.ACCOUNTS_PATH));

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(TestConstants.OAUTH_USER_PATH));
  }

  @Test
  @DisplayName("should redirect to error 403 when it retrieves malformed oauth response")
  public void shouldRedirectToError403WhenItRetrievesMalformedOAuthResponse() throws MalformedURLException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{}"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/error/403"));

    verifyHttp(githubStub)
            .once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().never(get(TestConstants.OAUTH_USER_EMAILS_PATH));
  }
}
