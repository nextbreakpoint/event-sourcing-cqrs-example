package com.nextbreakpoint.blueprint.authentication;

import com.jayway.restassured.RestAssured;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.*;
import static com.xebialabs.restito.semantics.Condition.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;

public class IntegrationTests {
  private static final String OAUTH_TOKEN_PATH = "/login/oauth/access_token";
  private static final String OAUTH_USER_PATH = "/user";
  private static final String OAUTH_USER_EMAILS_PATH = "/user/emails";
  private static final String ACCOUNTS_PATH = "/v1/accounts";
  private static final UUID ACCOUNT_UUID = new UUID(0, 1);

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
  @Tag("slow")
  @Tag("integration")
  @DisplayName("Verify behaviour of authentication service")
  public class VerifyServiceApi {
    @AfterEach
    public void reset() {
      RestAssured.reset();
      scenario.getStubServer().clear();
    }

    @Test
    @DisplayName("signin should redirect to login when user is not authenticated")
    public void signinShouldRedirectToLoginWhenUserIsNotAuthenticated() throws MalformedURLException {
      given().config(scenario.getRestAssuredConfig())
              .when().get(scenario.makeBaseURL("/v1/auth/signin/content/designs"))
              .then().assertThat().statusCode(302)
              .and().header("Location", containsString("/login/oauth/authorize?"));
    }

    @Test
    @DisplayName("should create an account and redirect to designs when authenticated user doesn't have an account")
    public void shouldCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserDoNotHaveAnAccount() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("{\"name\":\"test\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then(status(HttpStatus.OK_200), stringContent("[]"));

      whenHttp(scenario.getStubServer())
              .match(post(ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"))
              .then(status(HttpStatus.CREATED_201), stringContent("{\"role\":\"guest\", \"uuid\":\"" + ACCOUNT_UUID + "\"}"));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/content/designs"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().once(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
//            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then().once(post(ACCOUNTS_PATH), withPostBody(), withHeader("authorization"));
    }

    @Test
    @DisplayName("should not create an account and redirect to designs when authenticated user already has an account")
    public void shouldNotCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserAlreadyHasAnAccount() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then(status(HttpStatus.OK_200), stringContent("[\"" + ACCOUNT_UUID + "\"]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH + "/" + ACCOUNT_UUID))
              .then(status(HttpStatus.OK_200), stringContent("{\"role\":\"guest\", \"uuid\":\"" + ACCOUNT_UUID + "\"}"));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/content/designs"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().never(get(OAUTH_USER_PATH))
              .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then().once(get(ACCOUNTS_PATH + "/" + ACCOUNT_UUID));
    }

    @Test
    @DisplayName("should redirect to error 500 when it can't retrieve oauth data")
    public void shouldRedirectToError403WhenItCannotRetrieveOAuthData() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/500"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().never(get(OAUTH_USER_EMAILS_PATH));
    }

    @Test
    @DisplayName("should redirect to error 403 when it can't retrieve user data")
    public void shouldRedirectToError403WhenItCannotRetrieveUserData() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/403"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().never(get(OAUTH_USER_PATH))
              .then().never(post(ACCOUNTS_PATH));
    }

    @Test
    @DisplayName("should redirect to error 403 when it can't retrieve user account")
    public void shouldRedirectToError403WhenItCannotRetrieveUserAccount() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/403"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().never(get(OAUTH_USER_PATH))
              .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then().never(post(ACCOUNTS_PATH));
    }

    @Test
    @DisplayName("should redirect to error 500 when it retrieves a malformed list of accounts")
    public void shouldRedirectToError500WhenItRetrievesAMalformedListOfAcccounts() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then(status(HttpStatus.OK_200), stringContent("x"));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/500"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().never(get(OAUTH_USER_PATH))
              .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then().never(post(ACCOUNTS_PATH));
    }

    @Test
    @DisplayName("should redirect to error 403 when it can't create an account")
    public void shouldRedirectToError403WhenItCannotCreateAnAccount() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("{\"name\":\"test\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then(status(HttpStatus.OK_200), stringContent("[]"));

      whenHttp(scenario.getStubServer())
              .match(post(ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"))
              .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/403"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().once(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().once(post(ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"));
    }

    @Test
    @DisplayName("should redirect to error 403 when it can't retrieve an account")
    public void shouldRedirectToError403WhenItCannotRetrieveAnAccount() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("[{\"name\":\"test\"}]"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then(status(HttpStatus.OK_200), stringContent("[\"" + ACCOUNT_UUID + "\"]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH + "/" + ACCOUNT_UUID))
              .then(status(HttpStatus.INTERNAL_SERVER_ERROR_500));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/403"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().never(get(OAUTH_USER_PATH))
              .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then().once(get(ACCOUNTS_PATH + "/" + ACCOUNT_UUID));
    }

    @Test
    @DisplayName("should redirect to error 403 when it retrieves an invalid account")
    public void shouldRedirectToError403WhenItRetrievesAnInvalidAccount() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then(status(HttpStatus.OK_200), stringContent("[\"" + ACCOUNT_UUID + "\"]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH + "/" + ACCOUNT_UUID))
              .then(status(HttpStatus.OK_200), stringContent("{\"uuid\":\"" + ACCOUNT_UUID + "\"}"));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/403"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH + "/" + ACCOUNT_UUID))
              .then(status(HttpStatus.OK_200), stringContent("{\"role\":\"guest\"}"));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/403"));

      verifyHttp(scenario.getStubServer()).times(2, post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().never(get(OAUTH_USER_PATH))
              .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then().once(get(ACCOUNTS_PATH + "/" + ACCOUNT_UUID));
    }

    @Test
    @DisplayName("should redirect to error 500 when it retrieves a malformed account")
    public void shouldRedirectToError500WhenItRetrievesAMalformedAccount() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then(status(HttpStatus.OK_200), stringContent("[\"" + ACCOUNT_UUID + "\"]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH + "/" + ACCOUNT_UUID))
              .then(status(HttpStatus.OK_200), stringContent("x"));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/500"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().never(get(OAUTH_USER_PATH))
              .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then().once(get(ACCOUNTS_PATH + "/" + ACCOUNT_UUID));
    }

    @Test
    @DisplayName("should redirect to error 500 when it retrieves malformed user data")
    public void shouldRedirectToError500WhenItRetrievesMalformedUserData() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("x"));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/500"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().never(get(OAUTH_USER_PATH))
              .then().never(post(ACCOUNTS_PATH));
    }

    @Test
    @DisplayName("should redirect to error 403 when it retrieves malformed oauth response")
    public void shouldRedirectToError403WhenItRetrievesMalformedOAuthResponse() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{}"));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/error/403"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().never(get(OAUTH_USER_EMAILS_PATH));
    }

    @Test
    @DisplayName("should propagate the trace id when creating an account")
    public void shouldPropagateTheTraceIdWhenCreatingAnAccount() throws MalformedURLException {
      whenHttp(scenario.getStubServer())
              .match(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("{\"name\":\"test\"}"));

      whenHttp(scenario.getStubServer())
              .match(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then(status(HttpStatus.OK_200), stringContent("[{\"email\":\"test@localhost\", \"primary\":true}]"));

      whenHttp(scenario.getStubServer())
              .match(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then(status(HttpStatus.OK_200), stringContent("[]"));

      whenHttp(scenario.getStubServer())
              .match(post(ACCOUNTS_PATH), withPostBody(), withHeader("Authorization"))
              .then(status(HttpStatus.CREATED_201), stringContent("{\"role\":\"guest\", \"uuid\":\"" + ACCOUNT_UUID + "\"}"));

      given().config(scenario.getRestAssuredConfig())
              .with().param("code", "xxx")
              .and().param("state", "/v1/auth/signin/content/designs")
              .when().get(scenario.makeBaseURL("/v1/auth/callback"))
              .then().assertThat().statusCode(303)
              .and().header("Location", startsWith("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/content/designs"));

      verifyHttp(scenario.getStubServer()).once(post(OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
              .then().once(get(OAUTH_USER_EMAILS_PATH), withHeader("authorization", "Bearer abcdef"))
              .then().once(get(OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"))
//            .then().once(get(ACCOUNTS_PATH), parameter("email", "test@localhost"), withHeader("Authorization"))
              .then().once(post(ACCOUNTS_PATH), withPostBody(), withHeader("authorization"));
    }
  }
}
