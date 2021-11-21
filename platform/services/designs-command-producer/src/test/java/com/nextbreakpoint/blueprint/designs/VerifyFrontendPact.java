package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
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

@Tag("slow")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-command-producer and frontend")
@Provider("designs-command-producer")
@Consumer("frontend")
@PactBroker
public class VerifyFrontendPact {
    private static TestCases testCases = new TestCases("PactTests");

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        System.setProperty("http.port", "30121");

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
    public void before(PactVerificationContext context) {
        context.setTarget(new HttpsTestTarget(testCases.getScenario().getServiceHost(), Integer.parseInt(testCases.getScenario().getServicePort()), "/", true));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    @DisplayName("Verify interaction")
    public void pactVerificationTestTemplate(Pact pact, Interaction interaction, PactVerificationContext context, HttpRequest request) {
        System.out.println("TestTemplate called: " + pact.getProvider().getName() + ", " + interaction);
        final String authorization = testCases.getScenario().makeAuthorization(Authentication.NULL_USER_UUID, Authority.ADMIN);
        request.setHeader(Headers.AUTHORIZATION, authorization);
        context.verifyInteraction();
    }

    @State("kafka topic exists")
    public void kafkaTopicExists() {
    }
}
