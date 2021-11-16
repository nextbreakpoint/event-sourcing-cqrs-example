package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.AmpqTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.MalformedURLException;

public class PactTests {
    private static TestCases testCases = new TestCases("PactTests");

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        testCases.before();

        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.provider.version", testCases.getVersion());
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        testCases.after();
    }

    @Nested
    @Tag("slow")
    @Tag("pact")
    @DisplayName("Verify contract between designs-command-producer and designs-event-consumer")
    @Provider("designs-command-producer")
    @Consumer("designs-event-consumer")
    @PactBroker
    public class VerifyDesignsCommandConsumer {
        @BeforeEach
        public void before(PactVerificationContext context) {
            context.setTarget(new AmpqTestTarget());
        }

        @TestTemplate
        @ExtendWith(PactVerificationInvocationContextProvider.class)
        @DisplayName("Verify interaction")
        public void pactVerificationTestTemplate(PactVerificationContext context) {
        }

        @State("kafka topic exists")
        public void kafkaTopicExists() {
        }

        @PactVerifyProvider("design insert requested 1")
        public String produceDesignInsertRequested1() throws MalformedURLException {
            throw new RuntimeException("AAaAAAAAAaaaa");
//            return testCases.produceDesignInsertRequested();
        }

        @PactVerifyProvider("design insert requested 2")
        public String produceDesignInsertRequested2() throws MalformedURLException {
            throw new RuntimeException("AAaAAAAAAaaaa");
//            return testCases.produceDesignInsertRequested();
        }

        @PactVerifyProvider("design insert requested 3")
        public String produceDesignInsertRequested3() throws MalformedURLException {
            throw new RuntimeException("AAaAAAAAAaaaa");
//            return testCases.produceDesignInsertRequested();
        }

        @PactVerifyProvider("design update requested 1")
        public String produceDesignUpdateRequested1() throws MalformedURLException {
            throw new RuntimeException("AAaAAAAAAaaaa");
//            return testCases.produceDesignUpdateRequested();
        }

        @PactVerifyProvider("design delete requested 1")
        public String produceDesignDeleteRequested1() throws MalformedURLException {
            throw new RuntimeException("AAaAAAAAAaaaa");
//            return testCases.produceDesignDeleteRequested();
        }
    }
//
//    @Disabled
//    @Nested
//    @Tag("slow")
//    @Tag("pact")
//    @DisplayName("Verify contract between designs-command-producer and frontend")
//    @Provider("designs-command-producer")
//    @Consumer("frontend")
//    @PactBroker
//    public class VerifyFrontend {
//        @BeforeEach
//        public void before(PactVerificationContext context) {
//            context.setTarget(new HttpsTestTarget(testCases.getScenario().getServiceHost(), Integer.parseInt(testCases.getScenario().getServicePort()), "/", true));
//        }
//
//        @TestTemplate
//        @ExtendWith(PactVerificationInvocationContextProvider.class)
//        @DisplayName("Verify interaction")
//        public void pactVerificationTestTemplate(PactVerificationContext context, HttpRequest request) {
//            final String authorization = testCases.getScenario().makeAuthorization(Authentication.NULL_USER_UUID, Authority.ADMIN);
//            request.setHeader(Headers.AUTHORIZATION, authorization);
//            context.verifyInteraction();
//        }
//
//        @State("kafka topic exists")
//        public void kafkaTopicExists() {
//        }
//    }
}
