package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.nextbreakpoint.blueprint.common.core.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.*;

@Disabled
@Tag("slow")
@Tag("pact")
@DisplayName("Test designs-notification-dispatcher pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
    private static TestCases testCases = new TestCases("PactTests");

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        testCases.before();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        testCases.after();
    }

    @Pact(consumer = "designs-notification-dispatcher")
    public MessagePact designAggregateUpdateCompleted(MessagePactBuilder builder) {
        UUID uuid1 = new UUID(0L, 1L);
        UUID uuid2 = new UUID(0L, 2L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .stringValue("uuid", uuid1.toString())
                .stringMatcher("timestamp", "\\d{10}");

        PactDslJsonBody laypload1 = new PactDslJsonBody()
                .stringMatcher("messageId", uuid1.toString())
                .stringValue("messageType", "design-insert")
                .stringValue("messageBody", event1.toString())
                .stringValue("messageSource", "service-designs")
                .stringMatcher("partitionKey", TestConstants.UUID_REGEXP)
                .stringMatcher("timestamp", "\\d{10}");

        PactDslJsonBody event2 = new PactDslJsonBody()
                .stringValue("uuid", uuid2.toString())
                .stringMatcher("timestamp", "\\d{10}");

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("messageId", uuid2.toString())
                .stringValue("messageType", "design-insert")
                .stringValue("messageBody", event2.toString())
                .stringValue("messageSource", "service-designs")
                .stringMatcher("partitionKey", TestConstants.UUID_REGEXP)
                .stringMatcher("timestamp", "\\d{10}");

        return builder.given("kafka topic exists")
                .expectsToReceive("design aggregate update completed for design 00000000-0000-0000-0000-000000000001")
                .withContent(laypload1)
                .expectsToReceive("design aggregate update completed for design 00000000-0000-0000-0000-000000000002")
                .withContent(payload2)
                .toPact();
    }

    @Test
    @PactTestFor(providerName = "designs-event-consumer", port = "1111", pactMethod = "designAggregateUpdateCompleted", providerType = ProviderType.ASYNCH)
    @DisplayName("Should notify watchers of all resources after receiving a DesignAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(MessagePact messagePact) {
        final OutputMessage designAggregateUpdateCompletedMessage1 = TestUtils.toOutputMessage(messagePact.getMessages().get(0));

        final OutputMessage designAggregateUpdateCompletedMessage2 = TestUtils.toOutputMessage(messagePact.getMessages().get(1));

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(List.of(designAggregateUpdateCompletedMessage1, designAggregateUpdateCompletedMessage2));
    }

    @Test
    @PactTestFor(providerName = "designs-event-consumer", port = "1112", pactMethod = "designAggregateUpdateCompleted", providerType = ProviderType.ASYNCH)
    @DisplayName("Should notify watchers of single resource after receiving a DesignAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(MessagePact messagePact) {
        final OutputMessage designAggregateUpdateCompletedMessage1 = TestUtils.toOutputMessage(messagePact.getMessages().get(0));

        final OutputMessage designAggregateUpdateCompletedMessage2 = TestUtils.toOutputMessage(messagePact.getMessages().get(1));

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(List.of(designAggregateUpdateCompletedMessage1, designAggregateUpdateCompletedMessage2));
    }
}
