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
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_DRAFT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;

@Tag("docker")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-query and frontend")
@Provider("designs-query")
@Consumer("frontend")
@PactBroker
public class VerifyFrontendPact {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

  private static final TestCases testCases = new TestCases("DesignsQueryVerifyFrontendPact");

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
  public void thereAreSomeDesigns() {
    final String data1 = new JsonObject(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT1)).toString();
    final String data2 = new JsonObject(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT1)).toString();

    final Instant now = Instant.now();

    testCases.deleteDesigns();
    testCases.deleteDraftDesigns();

    final Design design1 = new Design(DESIGN_ID_1, USER_ID_1, UUID.randomUUID(), data1, Checksum.of(data1), REVISION_0, "CREATED", false, LEVELS_DRAFT, TestUtils.getTiles(LEVELS_DRAFT, 0.0f), FORMATTER.format(now.minusSeconds(10).truncatedTo(ChronoUnit.MILLIS)), FORMATTER.format(now.truncatedTo(ChronoUnit.MILLIS)));
    final Design design2 = new Design(DESIGN_ID_2, USER_ID_1, UUID.randomUUID(), data2, Checksum.of(data1), REVISION_1, "UPDATED", true, LEVELS_READY, TestUtils.getTiles(LEVELS_READY, 100.0f), FORMATTER.format(now.minusSeconds(5).truncatedTo(ChronoUnit.MILLIS)), FORMATTER.format(now.truncatedTo(ChronoUnit.MILLIS)));

    List.of(design1, design2).forEach(testCases::insertDraftDesign);
    List.of(design2).forEach(testCases::insertDesign);
  }

  @State("there is a published design")
  public void thereIsAPublishedDesign() {
    final String data = new JsonObject(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT1)).toString();

    final Instant now = Instant.now();

    testCases.deleteDesigns();
    testCases.deleteDraftDesigns();

    final Design design = new Design(DESIGN_ID_1, USER_ID_1, UUID.randomUUID(), data, Checksum.of(data), REVISION_0, "CREATED", true, LEVELS_READY, TestUtils.getTiles(LEVELS_READY, 100.0f), FORMATTER.format(now.minusSeconds(5).truncatedTo(ChronoUnit.MILLIS)), FORMATTER.format(now.truncatedTo(ChronoUnit.MILLIS)));

    List.of(design).forEach(testCases::insertDraftDesign);
    List.of(design).forEach(testCases::insertDesign);
  }
}
