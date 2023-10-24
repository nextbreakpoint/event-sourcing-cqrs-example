package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Headers;
import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("docker")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-command and frontend")
@Provider("designs-command")
@Consumer("frontend")
@PactBroker
public class VerifyFrontendPact {
    private static TestCases testCases = new TestCases("DesignsCommandVerifyFrontendPact");

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
    public void pactVerificationTestTemplate(Pact pact, Interaction interaction, PactVerificationContext context, HttpRequest request) {
        System.out.println("TestTemplate called: " + pact.getProvider().getName() + ", " + interaction);
        final String authorization = testCases.makeAuthorization(Authentication.NULL_USER_UUID, Authority.ADMIN);
        request.setHeader(Headers.AUTHORIZATION, authorization);
        context.verifyInteraction();
    }

    @State("kafka topic exists")
    public void kafkaTopicExists() {
    }
}
