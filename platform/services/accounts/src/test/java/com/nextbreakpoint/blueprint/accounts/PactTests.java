package com.nextbreakpoint.blueprint.accounts;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class PactTests {
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
  @Tag("slow")
  @Tag("pact")
  @DisplayName("Verify contract between accounts and authentication")
  @Provider("accounts")
  @Consumer("authentication")
  @PactBroker
  public class VerifyAuthentication {
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
    public void accountExistsForEmail() throws SQLException {
      try (Connection connection = DriverManager.getConnection(scenario.getMySqlConnectionUrl("accounts"), "root", "password")) {
        connection.prepareStatement("TRUNCATE ACCOUNTS;").execute();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO ACCOUNTS (UUID,NAME,EMAIL,ROLE) VALUES (?,?,?,?);");
        statement.setString(1, ACCOUNT_UUID.toString());
        statement.setString(2, "test");
        statement.setString(3, "test@localhost");
        statement.setString(4, "guest");
        statement.execute();
      }
    }

    @State("account exists for uuid")
    public void accountExistsForUuid() throws SQLException {
      try (Connection connection = DriverManager.getConnection(scenario.getMySqlConnectionUrl("accounts"), "root", "password")) {
        connection.prepareStatement("TRUNCATE ACCOUNTS;").execute();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO ACCOUNTS (UUID,NAME,EMAIL,ROLE) VALUES (?,?,?,?);");
        statement.setString(1, ACCOUNT_UUID.toString());
        statement.setString(2, "test");
        statement.setString(3, "test@localhost");
        statement.setString(4, "guest");
        statement.execute();
      }
    }

    @State("account doesn't exist")
    public void accountDoesNotExist() throws SQLException {
      try (Connection connection = DriverManager.getConnection(scenario.getMySqlConnectionUrl("accounts"), "root", "password")) {
        connection.prepareStatement("TRUNCATE ACCOUNTS;").execute();
      }
    }

    @State("user is authenticated")
    public void userHasAdminPermission() throws SQLException {
      try (Connection connection = DriverManager.getConnection(scenario.getMySqlConnectionUrl("accounts"), "root", "password")) {
        connection.prepareStatement("TRUNCATE ACCOUNTS;").execute();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO ACCOUNTS (UUID,NAME,EMAIL,ROLE) VALUES (?,?,?,?);");
        statement.setString(1, ACCOUNT_UUID.toString());
        statement.setString(2, "test");
        statement.setString(3, "test@localhost");
        statement.setString(4, "guest");
        statement.execute();
      }
    }
  }

  @Nested
  @Tag("slow")
  @Tag("pact")
  @DisplayName("Verify contract between accounts and frontend")
  @Provider("accounts")
  @Consumer("frontend")
  @PactBroker
  public class VerifyFrontend {
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
    public void accountExistsForUuid() throws SQLException {
      try (Connection connection = DriverManager.getConnection(scenario.getMySqlConnectionUrl("accounts"), "root", "password")) {
        connection.prepareStatement("TRUNCATE ACCOUNTS;").execute();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO ACCOUNTS (UUID,NAME,EMAIL,ROLE) VALUES (?,?,?,?);");
        statement.setString(1, ACCOUNT_UUID.toString());
        statement.setString(2, "test");
        statement.setString(3, "test@localhost");
        statement.setString(4, "guest");
        statement.execute();
      }
    }
  }
}
