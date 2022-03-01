package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("docker")
@Tag("pact")
@DisplayName("Test designs-notify pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
    private static TestCases testCases = new TestCases();

    @BeforeAll
    public static void before() {
        testCases.before();
    }

    @AfterAll
    public static void after() {
        testCases.after();
    }

    @Pact(consumer = "designs-notify")
    public MessagePact designDocumentUpdateCompleted(MessagePactBuilder builder) {
        UUID uuid1 = new UUID(0L, 1L);
        UUID uuid2 = new UUID(0L, 2L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", uuid1)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .stringMatcher("revision", TestConstants.UUID1_REGEXP);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .stringMatcher("token", TestConstants.UUID1_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.DESIGN_DOCUMET_UPDATE_COMPLETED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace1 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid1.toString())
                .object("value", payload1)
                .object("headers", trace1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("designId", uuid2)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .stringMatcher("revision", TestConstants.UUID1_REGEXP);

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .stringMatcher("token", TestConstants.UUID1_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.DESIGN_DOCUMET_UPDATE_COMPLETED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace2 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid2.toString())
                .object("value", payload2)
                .object("headers", trace2);

        return builder.given("kafka topic exists")
                .expectsToReceive("design document update completed for design 00000000-0000-0000-0000-000000000001")
                .withContent(message1)
                .expectsToReceive("design document update completed for design 00000000-0000-0000-0000-000000000002")
                .withContent(message2)
                .toPact();
    }

    @Test
    @PactTestFor(providerName = "designs-query", port = "1111", pactMethod = "designDocumentUpdateCompleted", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
    @DisplayName("Should notify watchers of all resources after receiving a DesignDocumentUpdateCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentUpdateCompletedEvent(MessagePact pact) {
        assertThat(pact.getMessages()).hasSize(2);

        final OutputMessage designDocumentUpdateCompletedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(0)));
        final OutputMessage designDocumentUpdateCompletedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(1)));

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(List.of(designDocumentUpdateCompletedMessage1, designDocumentUpdateCompletedMessage2));
    }

    @Test
    @PactTestFor(providerName = "designs-query", port = "1112", pactMethod = "designDocumentUpdateCompleted", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
    @DisplayName("Should notify watchers of single resource after receiving a DesignDocumentUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentUpdateCompletedEvent(MessagePact pact) {
        assertThat(pact.getMessages()).hasSize(2);

        final OutputMessage designDocumentUpdateCompletedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(0)));
        final OutputMessage designDocumentUpdateCompletedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(1)));

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(List.of(designDocumentUpdateCompletedMessage1, designDocumentUpdateCompletedMessage2));
    }
}
