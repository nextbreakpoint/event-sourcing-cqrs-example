package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Headers;
import com.nextbreakpoint.blueprint.designs.model.Design;
import io.vertx.core.json.JsonObject;
import org.apache.http.HttpRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Tag("docker")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-query and frontend")
@Provider("designs-query")
@Consumer("frontend")
@PactBroker
public class VerifyFrontendPact {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

  private static final TestCases testCases = new TestCases("PactTests");

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
  public void setup() {
    testCases.deleteDraftDesigns();
  }

  @BeforeEach
  public void before(PactVerificationContext context) {
    context.setTarget(testCases.getHttpTestTarget());
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  @DisplayName("Verify interaction")
  public void pactVerificationTestTemplate(PactVerificationContext context, HttpRequest request) {
    final String authorization = testCases.makeAuthorization("test", Authority.GUEST);
    request.setHeader(Headers.AUTHORIZATION, authorization);
    context.verifyInteraction();
  }

  @State("there are some designs")
  public void designsExist() {
    final String json1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();
    final String json2 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT2)).toString();

    final Instant now = Instant.now();

    final Design design1 = new Design(TestConstants.DESIGN_UUID_1, TestConstants.USER_ID, UUID.randomUUID(), json1, Checksum.of(json1), TestConstants.REVISION_0, "CREATED", false, TestConstants.LEVELS, TestUtils.getTiles(TestConstants.LEVELS, 0.0f), FORMATTER.format(now.minusSeconds(10)), FORMATTER.format(now));

    final Design design2 = new Design(TestConstants.DESIGN_UUID_2, TestConstants.USER_ID, UUID.randomUUID(), json2, Checksum.of(json1), TestConstants.REVISION_1, "UPDATED", false, TestConstants.LEVELS, TestUtils.getTiles(TestConstants.LEVELS, 0.5f), FORMATTER.format(now.minusSeconds(5)), FORMATTER.format(now));

    List.of(design1, design2).forEach(testCases::insertDraftDesign);
    List.of(design1, design2).forEach(testCases::insertDesign);
  }

  @State("design exists for uuid")
  public void designExistsForUuid() {
    final String json = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();

    final Instant now = Instant.now();

    final Design design = new Design(TestConstants.DESIGN_UUID_1, TestConstants.USER_ID, UUID.randomUUID(), json, Checksum.of(json), TestConstants.REVISION_0, "CREATED", false, TestConstants.LEVELS, TestUtils.getTiles(TestConstants.LEVELS, 0.0f), FORMATTER.format(now.minusSeconds(5)), FORMATTER.format(Instant.now()));

    List.of(design).forEach(testCases::insertDraftDesign);
    List.of(design).forEach(testCases::insertDesign);
  }
}
