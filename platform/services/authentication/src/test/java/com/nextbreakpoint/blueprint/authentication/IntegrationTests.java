package com.nextbreakpoint.blueprint.authentication;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.nextbreakpoint.blueprint.common.test.KubeUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.xebialabs.restito.server.StubServer;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_TRACE_ID;
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
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@Tag("integration")
@DisplayName("Authentication service")
public class IntegrationTests {
  private static final String OAUTH_TOKEN_PATH = "/login/oauth/access_token";
  private static final String OAUTH_USER_PATH = "/user";
  private static final String OAUTH_USER_EMAILS_PATH = "/user/emails";
  private static final String ACCOUNTS_PATH = "/v1/accounts";
  private static final String SOME_UUID = new UUID(0, 1).toString();

  private static final String version = "1.0.0-1";
  private static final String namespace = "integration";
  private static final long timestamp = System.currentTimeMillis();

  private static final String githubUsername = TestUtils.getVariable("GITHUB_USERNAME");
  private static final String githubPassword = TestUtils.getVariable("GITHUB_PASSWORD");

  private static boolean buildDockerImages = TestUtils.getVariable("BUILD_IMAGES", "false").equals("true");

  private static String httpPort = TestUtils.getVariable("HTTP_PORT", System.getProperty("http.port", "8080"));
  private static String stubPort = TestUtils.getVariable("STUB_PORT", System.getProperty("stub.port", "9000"));

  private static String minikubeHost;

  private static RestAssuredConfig restAssuredConfig;

  private static StubServer stubServer;

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    printInfo();
    configureMinikube();
    buildDockerImages();
    configureRestAssured();
    deleteNamespace();
    createNamespace();
    createSecrets();
    installServices();
    waitForService();
    exposeService();
    stubServer = new StubServer(Integer.parseInt(stubPort)).run();
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    if (stubServer != null) {
      stubServer.clear();
    }
    describeResources();
    printLogs();
    uninstallServices();
    deleteNamespace();
  }

  @BeforeEach
  public void setup() {
    if (stubServer != null) {
      stubServer.clear();
    }
  }

  @AfterEach
  public void reset() {
    RestAssured.reset();
  }

  @Test
  @DisplayName("signin should redirect to login when user is not authenticated")
  public void signinShouldRedirectToLoginWhenUserIsNotAuthenticated() throws MalformedURLException {
    given().config(restAssuredConfig)
            .when().get(makeBaseURL("/v1/auth/signin/content/designs"))
            .then().assertThat().statusCode(302)
            .and().header("Location", containsString("/login/oauth/authorize?"));
  }

  @Test
  @DisplayName("should create an account and redirect to designs when authenticated user doesn't have an account")
  public void shouldCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserDoNotHaveAnAccount() throws MalformedURLException {
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
            .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"), withHeader(X_TRACE_ID, "n/a"))
            .then(status(HttpStatus.OK_200), stringContent("[]"));

    whenHttp(stubServer)
            .match(post(ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"), withHeader(X_TRACE_ID, "n/a"))
            .then(status(HttpStatus.CREATED_201), stringContent("{\"role\":\"guest\", \"uuid\":\"" + SOME_UUID + "\"}"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/content/designs"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
//            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"), withHeader(X_TRACE_ID, "n/a"))
            .then().once(post(ACCOUNTS_PATH), withPostBody(), withHeader("authorization"), withHeader(X_TRACE_ID, "n/a"));
  }

  @Test
  @DisplayName("should not create an account and redirect to designs when authenticated user already has an account")
  public void shouldNotCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserAlreadyHasAnAccount() throws MalformedURLException {
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
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/content/designs"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(ACCOUNTS_PATH + "/" + SOME_UUID));
  }

  @Test
  @DisplayName("should redirect to error 500 when it can't retrieve oauth data")
  public void shouldRedirectToError403WhenItCannotRetrieveOAuthData() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/500"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().never(get(OAUTH_USER_EMAILS_PATH));
  }

  @Test
  @DisplayName("should redirect to error 403 when it can't retrieve user data")
  public void shouldRedirectToError403WhenItCannotRetrieveUserData() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/403"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().never(post(ACCOUNTS_PATH));
  }

  @Test
  @DisplayName("should redirect to error 403 when it can't retrieve user account")
  public void shouldRedirectToError403WhenItCannotRetrieveUserAccount() throws MalformedURLException {
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
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/403"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().never(post(ACCOUNTS_PATH));
  }

  @Test
  @DisplayName("should redirect to error 500 when it retrieves a malformed list of accounts")
  public void shouldRedirectToError500WhenItRetrievesAMalformedListOfAcccounts() throws MalformedURLException {
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
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/500"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().never(post(ACCOUNTS_PATH));
  }

  @Test
  @DisplayName("should redirect to error 403 when it can't create an account")
  public void shouldRedirectToError403WhenItCannotCreateAnAccount() throws MalformedURLException {
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
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/403"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(post(ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"));
  }

  @Test
  @DisplayName("should redirect to error 403 when it can't retrieve an account")
  public void shouldRedirectToError403WhenItCannotRetrieveAnAccount() throws MalformedURLException {
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
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/403"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(ACCOUNTS_PATH + "/" + SOME_UUID));
  }

  @Test
  @DisplayName("should redirect to error 403 when it retrieves an invalid account")
  public void shouldRedirectToError403WhenItRetrievesAnInvalidAccount() throws MalformedURLException {
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
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/403"));

    whenHttp(stubServer)
            .match(get(ACCOUNTS_PATH + "/" + SOME_UUID))
            .then(status(HttpStatus.OK_200), stringContent("{\"role\":\"guest\"}"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/403"));

    verifyHttp(stubServer).times(2, post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(ACCOUNTS_PATH + "/" + SOME_UUID));
  }

  @Test
  @DisplayName("should redirect to error 500 when it retrieves a malformed account")
  public void shouldRedirectToError500WhenItRetrievesAMalformedAccount() throws MalformedURLException {
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
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/500"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
            .then().once(get(ACCOUNTS_PATH + "/" + SOME_UUID));
  }

  @Test
  @DisplayName("should redirect to error 500 when it retrieves malformed user data")
  public void shouldRedirectToError500WhenItRetrievesMalformedUserData() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(stubServer)
            .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then(status(HttpStatus.OK_200), stringContent("x"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/500"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().never(get(OAUTH_USER_PATH))
            .then().never(post(ACCOUNTS_PATH));
  }

  @Test
  @DisplayName("should redirect to error 403 when it retrieves malformed oauth response")
  public void shouldRedirectToError403WhenItRetrievesMalformedOAuthResponse() throws MalformedURLException {
    whenHttp(stubServer)
            .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{}"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/error/403"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().never(get(OAUTH_USER_EMAILS_PATH));
  }

  @Test
  @DisplayName("should propagate the trace id when creating an account")
  public void shouldPropagateTheTraceIdWhenCreatingAnAccount() throws MalformedURLException {
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
            .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"), withHeader(X_TRACE_ID, "xxx"))
            .then(status(HttpStatus.OK_200), stringContent("[]"));

    whenHttp(stubServer)
            .match(post(ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"), withHeader(X_TRACE_ID, "xxx"))
            .then(status(HttpStatus.CREATED_201), stringContent("{\"role\":\"guest\", \"uuid\":\"" + SOME_UUID + "\"}"));

    given().config(restAssuredConfig)
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/content/designs")
            .and().header(X_TRACE_ID, "xxx")
            .when().get(makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith("https://" + minikubeHost + ":" + httpPort + "/content/designs"));

    verifyHttp(stubServer).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
            .then().once(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
//            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"), withHeader(X_TRACE_ID, "xxx"))
            .then().once(post(ACCOUNTS_PATH), withPostBody(), withHeader("authorization"), withHeader(X_TRACE_ID, "xxx"));
  }

  private static URL makeBaseURL(String path) throws MalformedURLException {
    final String normPath = path.startsWith("/") ? path.substring(1) : path;
    return new URL("https://" + minikubeHost + ":" + httpPort + "/" + normPath);
  }

  private static void configureRestAssured() {
    final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
    final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
    final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
    restAssuredConfig = RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
  }

  private static void printInfo() {
    System.out.println("Run test - " + new Date(timestamp));
    System.out.println("Namespace = " + namespace);
    System.out.println("Version = " + version);
    System.out.println("Build image = " + (buildDockerImages ? "Yes" : "No"));
  }

  private static void printLogs() throws IOException, InterruptedException {
    KubeUtils.printLogs(namespace, "authentication");
  }

  private static void describeResources() throws IOException, InterruptedException {
    KubeUtils.describePods(namespace);
  }

  private static void createNamespace() throws IOException, InterruptedException {
    if (KubeUtils.createNamespace(namespace) != 0) {
      fail("Can't create namespace");
    }
  }

  private static void deleteNamespace() throws IOException, InterruptedException {
    if (KubeUtils.deleteNamespace(namespace) != 0) {
      System.out.println("Can't delete namespace");
    }
  }

  private static void buildDockerImages() throws IOException, InterruptedException {
    if (!buildDockerImages) {
      return;
    }
    KubeUtils.cleanDockerImages();
    System.out.println("Building image...");
    List<String> args = Arrays.asList(
            "--build-arg", "github_username=" + githubUsername,
            "--build-arg", "github_password=" + githubPassword
    );
    if (KubeUtils.buildDockerImage(".", "integration/authentication:" + version, args) != 0) {
      fail("Can't build image");
    }
    System.out.println("Image created");
    buildDockerImages = false;
  }

  private static void installServices() throws IOException, InterruptedException {
    installService("authentication");
  }

  private static void uninstallServices() throws IOException, InterruptedException {
    uninstallService("authentication");
  }

  private static void installService(String name) throws IOException, InterruptedException {
    System.out.println("Installing service...");
    final List<String> args = Arrays.asList("--set=replicas=1,clientDomain=" + minikubeHost + ",clientWebUrl=https://" + minikubeHost + ":" + httpPort + ",clientAuthUrl=https://" + minikubeHost + ":" + httpPort + ",githubApiUrl=http://192.168.64.1:" + stubPort + ",githubOAuthUrl=http://192.168.64.1:" + stubPort + ",authApiUrl=http://192.168.64.1:" + stubPort + ",accountsApiUrl=http://192.168.64.1:" + stubPort + ",image.pullPolicy=Never,image.repository=integration/" + name + ",image.tag=" + version);
    if (KubeUtils.installHelmChart(namespace, "integration-" + name, "helm", args, true) != 0) {
      if (KubeUtils.upgradeHelmChart(namespace, "integration-" + name, "helm", args, true) != 0) {
        fail("Can't install or upgrade Helm chart");
      }
    }
    System.out.println("Service installed");
  }

  private static void uninstallService(String name) throws IOException, InterruptedException {
    System.out.println("Uninstalling service...");
    if (KubeUtils.uninstallHelmChart(namespace, "integration-" + name) != 0) {
      System.out.println("Can't uninstall Helm chart");
    }
    System.out.println("Service uninstalled");
  }

  private static void waitForService() {
    awaitUntilCondition(20, 5, 1, () -> isServiceReady(namespace, "authentication"));
  }

  private static boolean isServiceReady(String namespace, String name) throws IOException, InterruptedException {
    String logs = KubeUtils.fetchLogs(namespace, name);
    String[] lines = logs.split("\n");
    boolean serviceReady = Arrays.stream(lines).anyMatch(line -> line.contains("Succeeded in deploying verticle"));
    return serviceReady;
  }

  private static void createSecrets() throws IOException, InterruptedException {
    System.out.println("Creating secrets...");
    final List<String> args = Arrays.asList(
            "--from-file",
            "keystore_client.jks=../../secrets/keystore_client.jks",
            "--from-file",
            "truststore_client.jks=../../secrets/truststore_client.jks",
            "--from-file",
            "keystore_server.jks=../../secrets/keystore_server.jks",
            "--from-file",
            "keystore_auth.jceks=../../secrets/keystore_auth.jceks",
            "--from-literal",
            "KEYSTORE_SECRET=secret",
            "--from-literal",
            "GITHUB_ACCOUNT_ID=admin@localhost",
            "--from-literal",
            "GITHUB_CLIENT_ID=111",
            "--from-literal",
            "GITHUB_CLIENT_SECRET=222"
    );
    if (KubeUtils.createSecret(namespace,"authentication", args, true) != 0) {
      fail("Can't create secret");
    }
    System.out.println("Secrets created");
  }

  private static void exposeService() throws IOException, InterruptedException {
    System.out.println("Exposing service...");
    if (KubeUtils.exposeService(namespace,"authentication", Integer.parseInt(httpPort), 8080) != 0) {
      fail("Can't expose service");
    }
    System.out.println("Service exposed");
  }

  private static void configureMinikube() throws IOException, InterruptedException {
    minikubeHost = KubeUtils.getMinikubeIp();
  }

  public static void awaitUntilAsserted(Long timeout, Long delay, Long interval, ThrowingRunnable assertion) {
    Awaitility.await()
            .atMost(timeout, TimeUnit.SECONDS)
            .pollDelay(delay, TimeUnit.SECONDS)
            .pollInterval(interval, TimeUnit.SECONDS)
            .untilAsserted(assertion);
  }

  public static void awaitUntilCondition(int timeout, int delay, int interval, Callable<Boolean> condition) {
    Awaitility.await()
            .atMost(timeout, TimeUnit.SECONDS)
            .pollDelay(delay, TimeUnit.SECONDS)
            .pollInterval(interval, TimeUnit.SECONDS)
            .until(condition);
  }
}
