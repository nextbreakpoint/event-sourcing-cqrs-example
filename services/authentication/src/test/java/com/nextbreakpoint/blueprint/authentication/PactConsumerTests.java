package com.nextbreakpoint.blueprint.authentication;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.model.MockServerImplementation;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.xebialabs.restito.server.StubServer;
import io.restassured.RestAssured;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.nextbreakpoint.blueprint.authentication.TestConstants.ACCOUNT_UUID;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.startsWith;

@Tag("docker")
@Tag("pact")
@DisplayName("Test authentication pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
  private static final String GITHUB_API_VERSION = "2022-11-28";

  private static final TestCases testCases = new TestCases();

  private static final StubServer githubStub = new StubServer(Integer.parseInt("39002"));

  private final int expectedPort = 8000;
  private final String expectedHost = "localhost";
  private final String expectedProtocol = "http";

  @BeforeAll
  public static void before() {
    testCases.before();

    if (githubStub != null) {
      githubStub.start();
    }
  }

  @AfterAll
  public static void after() {
    testCases.after();

    if (githubStub != null) {
      githubStub.stop();
    }
  }

  @BeforeEach
  public void reset() {
    RestAssured.reset();

    if (githubStub != null) {
      githubStub.clear();
    }
  }

  @Pact(consumer = "authentication")
  public V4Pact accountExists(PactBuilder builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder.usingLegacyDsl()
            .given("account exists for uuid")
            .uponReceiving("request to retrieve accounts")
            .method("GET")
            .path("/v1/accounts")
            .matchQuery("login", "test-login")
            .matchHeader("Accept", "application/json", "application/json")
            .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                    new PactDslJsonArray()
                            .stringValue(ACCOUNT_UUID.toString())
            )
            .uponReceiving("request to fetch account")
            .method("GET")
            .path("/v1/accounts/" + ACCOUNT_UUID)
            .matchHeader("Accept", "application/json", "application/json")
            .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                    new PactDslJsonBody()
                            .stringValue("uuid", ACCOUNT_UUID.toString())
                            .stringValue("role", "guest")
            )
            .toPact(V4Pact.class);
  }

  @Pact(consumer = "authentication")
  public V4Pact accountDoesNotExist(PactBuilder builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder.usingLegacyDsl()
            .given("account doesn't exist")
            .uponReceiving("request to retrieve empty accounts")
            .method("GET")
            .path("/v1/accounts")
            .matchQuery("login", "test-login")
            .matchHeader("Accept", "application/json", "application/json")
            .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
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
            .matchHeader("Accept", "application/json", "application/json")
            .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
            .body(
                    new PactDslJsonBody()
                            .stringValue("login", "test-login")
                            .stringValue("name", "test")
                            .stringValue("role", "guest")
            )
            .willRespondWith()
            .headers(headers)
            .status(201)
            .body(
                    new PactDslJsonBody()
                            .stringMatcher("uuid", ".+", ACCOUNT_UUID.toString())
                            .stringValue("role", "guest")
            )
            .toPact(V4Pact.class);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  @PactTestFor(providerName = "accounts", pactMethod = "accountDoesNotExist", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "accounts", port = "39001", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should create an account and redirect to designs when authenticated user doesn't have an account")
  public void shouldCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserDoNotHaveAnAccount(MockServer mockServer) throws IOException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"),
                    withHeader("accept", "application/vnd.github+json"), withHeader("X-GitHub-Api-Version", GITHUB_API_VERSION))
            .then(status(HttpStatus.OK_200), stringContent("{\"name\":\"test\",\"login\":\"test-login\"}"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/some/content")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/some/content"));

    verifyHttp(githubStub).once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"),
                    withHeader("accept", "application/vnd.github+json"), withHeader("X-GitHub-Api-Version", GITHUB_API_VERSION));
  }

  @Test
  @EnabledOnOs(OS.MAC)
  @PactTestFor(providerName = "accounts", pactMethod = "accountExists", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "accounts", port = "39001", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should not create an account and redirect to designs when authenticated user already has an account")
  public void shouldNotCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserAlreadyHasAnAccount(MockServer mockServer) throws IOException {
    whenHttp(githubStub)
            .match(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"access_token\":\"abcdef\"}"));

    whenHttp(githubStub)
            .match(get(TestConstants.OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"),
            withHeader("accept", "application/vnd.github+json"), withHeader("X-GitHub-Api-Version", GITHUB_API_VERSION))
            .then(status(HttpStatus.OK_200), stringContent("{\"name\":\"test\",\"login\":\"test-login\"}"));

    given().config(TestUtils.getRestAssuredConfig())
            .with().param("code", "xxx")
            .and().param("state", "/v1/auth/signin/some/content")
            .when().get(testCases.makeBaseURL("/v1/auth/callback"))
            .then().assertThat().statusCode(303)
            .and().header("Location", startsWith(expectedProtocol + "://" + expectedHost + ":" + expectedPort + "/some/content"));

    verifyHttp(githubStub).once(post(TestConstants.OAUTH_TOKEN_PATH), withHeader("accept", "application/json,application/x-www-form-urlencoded;q=0.9"))
            .then().once(get(TestConstants.OAUTH_USER_PATH), withHeader("authorization", "Bearer abcdef"),
                    withHeader("accept", "application/vnd.github+json"), withHeader("X-GitHub-Api-Version", GITHUB_API_VERSION));
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @PactTestFor(providerName = "accounts", pactMethod = "accountDoesNotExist", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "accounts", port = "39001", hostInterface = "172.17.0.1", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should create an account and redirect to designs when authenticated user doesn't have an account")
  public void shouldCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserDoNotHaveAnAccountWorkaround(MockServer mockServer) throws IOException {
    shouldCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserDoNotHaveAnAccount(mockServer);
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @PactTestFor(providerName = "accounts", pactMethod = "accountExists", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "accounts", port = "39001", hostInterface = "172.17.0.1", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should not create an account and redirect to designs when authenticated user already has an account")
  public void shouldNotCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserAlreadyHasAnAccountWorkaround(MockServer mockServer) throws IOException {
    shouldNotCreateAnAccountAndRedirectToDesignsWhenAuthenticatedUserAlreadyHasAnAccount(mockServer);
  }
}
