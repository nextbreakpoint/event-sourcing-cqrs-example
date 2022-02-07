package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

@Tag("slow")
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
                .uuid("uuid", uuid1)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.DESIGN_DOCUMET_UPDATE_COMPLETED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid1.toString())
                .object("value", payload1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("uuid", uuid2)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP);

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.DESIGN_DOCUMET_UPDATE_COMPLETED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid2.toString())
                .object("value", payload2);

        return builder.given("kafka topic exists")
                .expectsToReceive("design aggregate update completed for design 00000000-0000-0000-0000-000000000001")
                .withContent(message1)
                .expectsToReceive("design aggregate update completed for design 00000000-0000-0000-0000-000000000002")
                .withContent(message2)
                .toPact();
    }

    @Test
    @PactTestFor(providerName = "designs-aggregate", port = "1111", pactMethod = "designDocumentUpdateCompleted", providerType = ProviderType.ASYNCH)
    @DisplayName("Should notify watchers of all resources after receiving a DesignDocumentUpdateCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentUpdateCompletedEvent(MessagePact messagePact) {
        final OutputMessage designDocumentUpdateCompletedMessage1 = TestUtils.toOutputMessage(messagePact.getMessages().get(0));

        final OutputMessage designDocumentUpdateCompletedMessage2 = TestUtils.toOutputMessage(messagePact.getMessages().get(1));

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(List.of(designDocumentUpdateCompletedMessage1, designDocumentUpdateCompletedMessage2));
    }

    @Test
    @PactTestFor(providerName = "designs-aggregate", port = "1112", pactMethod = "designDocumentUpdateCompleted", providerType = ProviderType.ASYNCH)
    @DisplayName("Should notify watchers of single resource after receiving a DesignDocumentUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentUpdateCompletedEvent(MessagePact messagePact) {
        final OutputMessage designDocumentUpdateCompletedMessage1 = TestUtils.toOutputMessage(messagePact.getMessages().get(0));

        final OutputMessage designDocumentUpdateCompletedMessage2 = TestUtils.toOutputMessage(messagePact.getMessages().get(1));

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(List.of(designDocumentUpdateCompletedMessage1, designDocumentUpdateCompletedMessage2));
    }
}
