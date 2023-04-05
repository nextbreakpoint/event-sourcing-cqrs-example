package com.nextbreakpoint.blueprint.accounts;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Headers;
import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import org.apache.http.HttpRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Tag("docker")
@Tag("pact-verify")
@DisplayName("Verify contract between accounts and authentication")
@Provider("accounts")
@Consumer("authentication")
@PactBroker
public class VerifyAuthenticationPact {
  private static final TestCases testCases = new TestCases();

  @BeforeAll
  public static void before() {
    testCases.before();

    System.setProperty("pact.showStacktrace", "true");
    System.setProperty("pact.verifier.publishResults", "true");
    System.setProperty("pact.provider.version", testCases.getVersion());
  }

  @AfterAll
  public static void after() {
    testCases.after();
  }

  @BeforeEach
  public void before(PactVerificationContext context) {
    context.setTarget(testCases.getHttpTestTarget());
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  @DisplayName("Verify interaction")
  public void pactVerificationTestTemplate(PactVerificationContext context, HttpRequest request) {
    final String authorization = testCases.makeAuthorization(Authentication.NULL_USER_UUID, Authority.PLATFORM);
    request.setHeader(Headers.AUTHORIZATION, authorization);
    context.verifyInteraction();
  }

  @State("account exists for email")
  public void accountExistsForEmail() throws SQLException {
    try (Connection connection = DriverManager.getConnection(testCases.getMySqlConnectionUrl(TestConstants.DATABASE_NAME), TestConstants.DATABASE_USERNAME, TestConstants.DATABASE_PASSWORD)) {
      connection.prepareStatement("TRUNCATE ACCOUNT;").execute();
      PreparedStatement statement = connection.prepareStatement("INSERT INTO ACCOUNT (ACCOUNT_UUID,ACCOUNT_NAME,ACCOUNT_EMAIL,ACCOUNT_AUTHORITIES,ACCOUNT_CREATED) VALUES (?,?,?,?,CURRENT_TIMESTAMP);");
      statement.setString(1, TestConstants.ACCOUNT_UUID.toString());
      statement.setString(2, "test");
      statement.setString(3, "test@localhost");
      statement.setString(4, "guest");
      statement.execute();
    }
  }

  @State("account exists for uuid")
  public void accountExistsForUuid() throws SQLException {
    try (Connection connection = DriverManager.getConnection(testCases.getMySqlConnectionUrl(TestConstants.DATABASE_NAME), TestConstants.DATABASE_USERNAME, TestConstants.DATABASE_PASSWORD)) {
      connection.prepareStatement("TRUNCATE ACCOUNT;").execute();
      PreparedStatement statement = connection.prepareStatement("INSERT INTO ACCOUNT (ACCOUNT_UUID,ACCOUNT_NAME,ACCOUNT_EMAIL,ACCOUNT_AUTHORITIES,ACCOUNT_CREATED) VALUES (?,?,?,?,CURRENT_TIMESTAMP);");
      statement.setString(1, TestConstants.ACCOUNT_UUID.toString());
      statement.setString(2, "test");
      statement.setString(3, "test@localhost");
      statement.setString(4, "guest");
      statement.execute();
    }
  }

  @State("account doesn't exist")
  public void accountDoesNotExist() throws SQLException {
    try (Connection connection = DriverManager.getConnection(testCases.getMySqlConnectionUrl(TestConstants.DATABASE_NAME), TestConstants.DATABASE_USERNAME, TestConstants.DATABASE_PASSWORD)) {
      connection.prepareStatement("TRUNCATE ACCOUNT;").execute();
    }
  }

  @State("user is authenticated")
  public void userHasAdminPermission() throws SQLException {
    try (Connection connection = DriverManager.getConnection(testCases.getMySqlConnectionUrl(TestConstants.DATABASE_NAME), TestConstants.DATABASE_USERNAME, TestConstants.DATABASE_PASSWORD)) {
      connection.prepareStatement("TRUNCATE ACCOUNT;").execute();
      PreparedStatement statement = connection.prepareStatement("INSERT INTO ACCOUNT (ACCOUNT_UUID,ACCOUNT_NAME,ACCOUNT_EMAIL,ACCOUNT_AUTHORITIES,ACCOUNT_CREATED) VALUES (?,?,?,?,CURRENT_TIMESTAMP);");
      statement.setString(1, TestConstants.ACCOUNT_UUID.toString());
      statement.setString(2, "test");
      statement.setString(3, "test@localhost");
      statement.setString(4, "guest");
      statement.execute();
    }
  }
}
