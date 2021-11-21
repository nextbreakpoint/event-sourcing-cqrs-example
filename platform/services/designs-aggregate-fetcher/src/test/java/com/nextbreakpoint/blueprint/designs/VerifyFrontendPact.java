package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Headers;
import io.vertx.core.json.JsonObject;
import org.apache.http.HttpRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Tag("slow")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-aggregate-fetcher and frontend")
@Provider("designs-aggregate-fetcher")
@Consumer("frontend")
@PactBroker
public class VerifyFrontendPact {
  private static final TestCases testCases = new TestCases();

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    System.setProperty("http.port", "30120");

    testCases.before();

    System.setProperty("pact.showStacktrace", "true");
    System.setProperty("pact.verifier.publishResults", "true");
    System.setProperty("pact.provider.version", testCases.getVersion());
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    testCases.after();
  }

  @BeforeEach
  public void setup() {
    testCases.deleteDesigns();
  }

  @BeforeEach
  public void before(PactVerificationContext context) {
    context.setTarget(new HttpsTestTarget(testCases.getScenario().getServiceHost(), Integer.parseInt(testCases.getScenario().getServicePort()), "/", true));
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  @DisplayName("Verify interaction")
  public void pactVerificationTestTemplate(PactVerificationContext context, HttpRequest request) {
    final String authorization = testCases.getScenario().makeAuthorization("test", Authority.GUEST);
    request.setHeader(Headers.AUTHORIZATION, authorization);
    context.verifyInteraction();
  }

  @State("there are some designs")
  public void designsExist() {
    final String json1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();
    final String json2 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT2)).toString();

    final Design design1 = new Design(Uuids.timeBased(), TestConstants.DESIGN_UUID_1, 0, json1, Checksum.of(json1), "CREATED", TestConstants.LEVELS, new ArrayList<>(), new Date());
    final Design design2 = new Design(Uuids.timeBased(), TestConstants.DESIGN_UUID_2, 1, json2, Checksum.of(json1), "UPDATED", TestConstants.LEVELS, new ArrayList<>(), new Date());

    List.of(design1, design2).forEach(testCases::insertDesign);
  }

  @State("design exists for uuid")
  public void designExistsForUuid() {
    final String json1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();

    final Design design1 = new Design(Uuids.timeBased(), TestConstants.DESIGN_UUID_1, 0, json1, Checksum.of(json1), "CREATED", TestConstants.LEVELS, new ArrayList<>(), new Date());

    List.of(design1).forEach(testCases::insertDesign);
  }
}
