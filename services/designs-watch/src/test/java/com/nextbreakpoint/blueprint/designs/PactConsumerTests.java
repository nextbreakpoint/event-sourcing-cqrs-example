package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_DELETE_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_UPDATE_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_REGEXP;
import static com.nextbreakpoint.blueprint.designs.TestConstants.UUID6_REGEXP;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("docker")
@Tag("pact")
@DisplayName("Test designs-watch pact")
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

    @BeforeEach
    public void beforeEach() {
        testCases.getSteps().reset();
    }

    @Pact(consumer = "designs-watch")
    public V4Pact shouldNotifyWatchersWhenReceivingADesignDocumentUpdateCompletedEvent(MessagePactBuilder builder) {
        return builder.given("kafka topic exists")
                .expectsToReceive("design document update completed for design 00000000-0000-0000-0000-000000000001")
                .withContent(getDesignDocumentUpdateCompletedMessage(DESIGN_ID_1, COMMAND_ID_1, new UUID(2L, 1L), REVISION_0))
                .expectsToReceive("design document update completed for design 00000000-0000-0000-0000-000000000002")
                .withContent(getDesignDocumentUpdateCompletedMessage(DESIGN_ID_2, COMMAND_ID_2, new UUID(2L, 2L), REVISION_1))
                .toPact();
    }

    @Pact(consumer = "designs-watch")
    public V4Pact shouldNotifyWatchersWhenReceivingADesignDocumentDeleteCompletedEvent(MessagePactBuilder builder) {
        return builder.given("kafka topic exists")
                .expectsToReceive("design document delete completed for design 00000000-0000-0000-0000-000000000001")
                .withContent(getDesignDocumentDeleteCompletedMessage(DESIGN_ID_1, COMMAND_ID_1, new UUID(2L, 1L), REVISION_0))
                .expectsToReceive("design document delete completed for design 00000000-0000-0000-0000-000000000002")
                .withContent(getDesignDocumentDeleteCompletedMessage(DESIGN_ID_2, COMMAND_ID_2, new UUID(2L, 2L), REVISION_1))
                .toPact();
    }

    @NotNull
    private static PactDslJsonBody getDesignDocumentUpdateCompletedMessage(UUID designId, UUID commandId, UUID eventId, String revision) {
        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", designId)
                .stringMatcher("commandId", UUID6_REGEXP, commandId.toString())
                .stringMatcher("revision", REVISION_REGEXP, revision);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", UUID6_REGEXP, eventId.toString())
                .object("data", event1)
                .stringValue("type", DESIGN_DOCUMENT_UPDATE_COMPLETED)
                .stringValue("source", MESSAGE_SOURCE);

        return new PactDslJsonBody()
                .stringValue("key", designId.toString())
                .object("value", payload1);
    }

    @NotNull
    private static PactDslJsonBody getDesignDocumentDeleteCompletedMessage(UUID designId, UUID commandId, UUID eventId, String revision) {
        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", designId)
                .stringMatcher("commandId", UUID6_REGEXP, commandId.toString())
                .stringMatcher("revision", REVISION_REGEXP, revision);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", UUID6_REGEXP, eventId.toString())
                .object("data", event1)
                .stringValue("type", DESIGN_DOCUMENT_DELETE_COMPLETED)
                .stringValue("source", MESSAGE_SOURCE);

        return new PactDslJsonBody()
                .stringValue("key", designId.toString())
                .object("value", payload1);
    }

    @Test
    @PactTestFor(providerName = "designs-query", pactMethod = "shouldNotifyWatchersWhenReceivingADesignDocumentUpdateCompletedEvent", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should notify watchers of all resources after receiving a DesignDocumentUpdateCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentUpdateCompletedEvent(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(2);

        final OutputMessage<DesignDocumentUpdateCompleted> designDocumentUpdateCompletedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()), DesignDocumentUpdateCompleted.class);
        final OutputMessage<DesignDocumentUpdateCompleted> designDocumentUpdateCompletedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()), DesignDocumentUpdateCompleted.class);

        final List<OutputMessage<DesignDocumentUpdateCompleted>> designDocumentUpdateCompletedMessages = List.of(
                designDocumentUpdateCompletedMessage1,
                designDocumentUpdateCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentUpdateCompletedEvent(designDocumentUpdateCompletedMessages);
    }

    @Test
    @PactTestFor(providerName = "designs-query", pactMethod = "shouldNotifyWatchersWhenReceivingADesignDocumentUpdateCompletedEvent", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should notify watchers of single resource after receiving a DesignDocumentUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentUpdateCompletedEvent(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(2);

        final OutputMessage<DesignDocumentUpdateCompleted> designDocumentUpdateCompletedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()), DesignDocumentUpdateCompleted.class);
        final OutputMessage<DesignDocumentUpdateCompleted> designDocumentUpdateCompletedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()), DesignDocumentUpdateCompleted.class);

        final List<OutputMessage<DesignDocumentUpdateCompleted>> designDocumentUpdateCompletedMessages = List.of(
                designDocumentUpdateCompletedMessage1,
                designDocumentUpdateCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentUpdateCompletedEvent(designDocumentUpdateCompletedMessages, DESIGN_ID_1);
    }

    @Test
    @PactTestFor(providerName = "designs-query", pactMethod = "shouldNotifyWatchersWhenReceivingADesignDocumentDeleteCompletedEvent", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should notify watchers of all resources after receiving a DesignDocumentDeleteCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentDeleteCompletedEvent(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(2);

        final OutputMessage<DesignDocumentDeleteCompleted> designDocumentDeleteCompletedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()), DesignDocumentDeleteCompleted.class);
        final OutputMessage<DesignDocumentDeleteCompleted> designDocumentDeleteCompletedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()), DesignDocumentDeleteCompleted.class);

        final List<OutputMessage<DesignDocumentDeleteCompleted>> designDocumentDeleteCompletedMessages = List.of(
                designDocumentDeleteCompletedMessage1,
                designDocumentDeleteCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentDeleteCompletedEvent(designDocumentDeleteCompletedMessages);
   }

    @Test
    @PactTestFor(providerName = "designs-query", pactMethod = "shouldNotifyWatchersWhenReceivingADesignDocumentDeleteCompletedEvent", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should notify watchers of single resource after receiving a DesignDocumentDeleteCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentDeleteCompletedEvent(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(2);

        final OutputMessage<DesignDocumentDeleteCompleted> designDocumentDeleteCompletedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()), DesignDocumentDeleteCompleted.class);
        final OutputMessage<DesignDocumentDeleteCompleted> designDocumentDeleteCompletedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()), DesignDocumentDeleteCompleted.class);

        final List<OutputMessage<DesignDocumentDeleteCompleted>> designDocumentDeleteCompletedMessages = List.of(
                designDocumentDeleteCompletedMessage1,
                designDocumentDeleteCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentDeleteCompletedEvent(designDocumentDeleteCompletedMessages, DESIGN_ID_1);
    }
}
