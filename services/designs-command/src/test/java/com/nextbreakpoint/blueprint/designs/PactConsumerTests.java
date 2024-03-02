package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.model.MockServerImplementation;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.nextbreakpoint.blueprint.common.core.Authority;
import io.restassured.RestAssured;
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

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_INSERT_COMMAND;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_UPDATE_COMMAND;
import static com.nextbreakpoint.blueprint.designs.TestConstants.INVALID_MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;

@Tag("docker")
@Tag("pact")
@DisplayName("Test designs-command pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
  private static final TestCases testCases = new TestCases("PactTests");

  @BeforeAll
  public static void before() {
    testCases.before();
  }

  @AfterAll
  public static void after() {
    testCases.after();
  }

  @BeforeEach
  public void reset() {
    RestAssured.reset();

    testCases.deleteData();
    testCases.getSteps().reset();
  }

  @Pact(consumer = "designs-command")
  public V4Pact designIsAccepted(PactBuilder builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder.usingLegacyDsl()
            .uponReceiving("request to validate a valid design")
            .method("POST")
            .path("/v1/designs/validate")
            .matchHeader("Accept", "application/json", "application/json")
            .matchHeader("Content-Type", "application/json", "application/json")
            .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
            .body(
                    new PactDslJsonBody()
                            .stringValue("manifest", MANIFEST)
                            .stringValue("metadata", METADATA)
                            .stringValue("script", SCRIPT)
            )
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                    new PactDslJsonBody()
                            .stringValue("status", "ACCEPTED")
                            .array("errors")
            )
            .toPact(V4Pact.class);
  }

  @Pact(consumer = "designs-command")
  public V4Pact designIsRejected(PactBuilder builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder.usingLegacyDsl()
            .uponReceiving("request to validate an invalid design")
            .method("POST")
            .path("/v1/designs/validate")
            .matchHeader("Accept", "application/json", "application/json")
            .matchHeader("Content-Type", "application/json", "application/json")
            .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
            .body(
                    new PactDslJsonBody()
                            .stringValue("manifest", INVALID_MANIFEST)
                            .stringValue("metadata", METADATA)
                            .stringValue("script", SCRIPT)
            )
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                    new PactDslJsonBody()
                            .stringValue("status", "REJECTED")
                            .array("errors")
                            .stringValue("Factory not found none")
            )
            .toPact(V4Pact.class);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  @PactTestFor(providerName = "designs-render", pactMethod = "designIsAccepted", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "designs-render", port = "39001", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should produce an insert command when user is authenticated and design is accepted")
  public void shouldProduceAnInsertDesignCommandWhenUserIsAuthenticatedAndDesignIsAccepted(MockServer mockServer) throws IOException {
    testCases.getSteps()
            .given().theUserId(USER_ID_1)
            .and().theManifest(MANIFEST)
            .and().theMetadata(METADATA)
            .and().theScript(SCRIPT)
            .and().anAuthorization(Authority.ADMIN)
            .when().discardReceivedCommands()
            .and().discardReceivedEvents()
            .and().submitInsertDesignRequest()
            .then().requestIsAccepted()
            .and().responseContainsDesignId()
            .and().aCommandMessageShouldBePublished(DESIGN_INSERT_COMMAND)
            .and().aDesignInsertCommandMessageShouldBeSaved()
            .and().aDesignInsertRequestedMessageShouldBePublished()
            .and().theDesignInsertRequestedEventShouldHaveExpectedValues();
  }

  @Test
  @EnabledOnOs(OS.MAC)
  @PactTestFor(providerName = "designs-render", pactMethod = "designIsRejected", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "designs-render", port = "39001", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should not produce an insert command when user is authenticated and design is rejected")
  public void shouldNotProduceAnInsertDesignCommandWhenUserIsAuthenticatedAndDesignIsRejected(MockServer mockServer) throws IOException {
    testCases.getSteps()
            .given().theUserId(USER_ID_1)
            .and().theManifest(INVALID_MANIFEST)
            .and().theMetadata(METADATA)
            .and().theScript(SCRIPT)
            .and().anAuthorization(Authority.ADMIN)
            .when().discardReceivedCommands()
            .and().discardReceivedEvents()
            .and().submitInsertDesignRequest()
            .then().requestIsRejected();
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @PactTestFor(providerName = "designs-render", pactMethod = "designIsAccepted", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "designs-render", port = "39001", hostInterface = "172.17.0.1", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should produce an insert command when user is authenticated and design is accepted")
  public void shouldProduceAnInsertDesignCommandWhenUserIsAuthenticatedAndDesignIsAcceptedWorkaround(MockServer mockServer) throws IOException {
    shouldProduceAnInsertDesignCommandWhenUserIsAuthenticatedAndDesignIsAccepted(mockServer);
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @PactTestFor(providerName = "designs-render", pactMethod = "designIsRejected", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "designs-render", port = "39001", hostInterface = "172.17.0.1", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should not produce an insert command when user is authenticated and design is rejected")
  public void shouldNotProduceAnInsertDesignCommandWhenUserIsAuthenticatedAndDesignIsRejectedWorkaround(MockServer mockServer) throws IOException {
    shouldNotProduceAnInsertDesignCommandWhenUserIsAuthenticatedAndDesignIsRejected(mockServer);
  }

  @Test
  @EnabledOnOs(OS.MAC)
  @PactTestFor(providerName = "designs-render", pactMethod = "designIsAccepted", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "designs-render", port = "39001", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should produce an update command when user is authenticated and design is accepted")
  public void shouldProduceAnUpdateDesignCommandWhenUserIsAuthenticatedAndDesignIsAccepted(MockServer mockServer) throws IOException {
    testCases.getSteps()
            .given().theUserId(USER_ID_1)
            .and().theDesignId(DESIGN_ID_1)
            .and().theManifest(MANIFEST)
            .and().theMetadata(METADATA)
            .and().theScript(SCRIPT)
            .and().anAuthorization(Authority.ADMIN)
            .when().discardReceivedCommands()
            .and().discardReceivedEvents()
            .and().submitUpdateDesignRequest()
            .then().requestIsAccepted()
            .and().responseContainsDesignId()
            .and().aCommandMessageShouldBePublished(DESIGN_UPDATE_COMMAND)
            .and().aDesignUpdateCommandMessageShouldBeSaved()
            .and().aDesignUpdateRequestedMessageShouldBePublished()
            .and().theDesignUpdateRequestedEventShouldHaveExpectedValues();
  }

  @Test
  @EnabledOnOs(OS.MAC)
  @PactTestFor(providerName = "designs-render", pactMethod = "designIsRejected", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "designs-render", port = "39001", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should not produce an update command when user is authenticated and design is rejected")
  public void shouldNotProduceAnUpdateDesignCommandWhenUserIsAuthenticatedAndDesignIsRejected(MockServer mockServer) throws IOException {
    testCases.getSteps()
            .given().theUserId(USER_ID_1)
            .and().theDesignId(DESIGN_ID_1)
            .and().theManifest(INVALID_MANIFEST)
            .and().theMetadata(METADATA)
            .and().theScript(SCRIPT)
            .and().anAuthorization(Authority.ADMIN)
            .when().discardReceivedCommands()
            .and().discardReceivedEvents()
            .and().submitUpdateDesignRequest()
            .then().requestIsRejected();
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @PactTestFor(providerName = "designs-render", pactMethod = "designIsAccepted", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "designs-render", port = "39001", hostInterface = "172.17.0.1", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should produce an update command when user is authenticated and design is accepted")
  public void shouldProduceAnUpdateDesignCommandWhenUserIsAuthenticatedAndDesignIsAcceptedWorkaround(MockServer mockServer) throws IOException {
    shouldProduceAnUpdateDesignCommandWhenUserIsAuthenticatedAndDesignIsAccepted(mockServer);
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @PactTestFor(providerName = "designs-render", pactMethod = "designIsRejected", pactVersion = PactSpecVersion.V4)
  @MockServerConfig(providerName = "designs-render", port = "39001", hostInterface = "172.17.0.1", implementation = MockServerImplementation.KTorServer)
  @DisplayName("should not produce an update command when user is authenticated and design is rejected")
  public void shouldNotProduceAnUpdateDesignCommandWhenUserIsAuthenticatedAndDesignIsRejectedWorkaround(MockServer mockServer) throws IOException {
    shouldNotProduceAnUpdateDesignCommandWhenUserIsAuthenticatedAndDesignIsRejected(mockServer);
  }
}
