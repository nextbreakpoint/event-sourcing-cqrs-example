package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_4;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_4;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_5;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_DRAFT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_REGEXP;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.UUID6_REGEXP;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("docker")
@Tag("pact")
@DisplayName("Test designs-query pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
    private static TestCases testCases = new TestCases("DesignsQueryPactConsumerTests");

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
    public void beforeEach() {
        testCases.deleteData();
        testCases.getSteps().reset();
    }

    @Pact(consumer = "designs-query", provider = "designs-aggregate")
    public V4Pact shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequestedMessage(MessagePactBuilder builder) {
        return builder
                .given("kafka topic exists")
                .expectsToReceive("design document update requested for design " + DESIGN_ID_4 + " with 0% tiles completed and not published")
                .withContent(getDesignDocumentUpdateRequestedMessage(DESIGN_ID_4, COMMAND_ID_1, new UUID(2L, 1L), REVISION_0, USER_ID_1, DATA_1, "CREATED", LEVELS_DRAFT, getCompletedTiles(LEVELS_DRAFT, 0), false))
                .expectsToReceive("design document update requested for design " + DESIGN_ID_4 + " with 50% tiles completed and not published")
                .withContent(getDesignDocumentUpdateRequestedMessage(DESIGN_ID_4, COMMAND_ID_2, new UUID(2L, 2L), REVISION_1, USER_ID_1, DATA_2, "UPDATED", LEVELS_DRAFT, getCompletedTiles(LEVELS_DRAFT, 50), false))
                .expectsToReceive("design document update requested for design " + DESIGN_ID_4 + " with 100% tiles completed and not published")
                .withContent(getDesignDocumentUpdateRequestedMessage(DESIGN_ID_4, COMMAND_ID_3, new UUID(2L, 3L), REVISION_2, USER_ID_1, DATA_2, "UPDATED", LEVELS_DRAFT, getCompletedTiles(LEVELS_DRAFT, 100), false))
                .expectsToReceive("design document update requested for design " + DESIGN_ID_4 + " with 100% tiles completed and published")
                .withContent(getDesignDocumentUpdateRequestedMessage(DESIGN_ID_4, COMMAND_ID_4, new UUID(2L, 4L), REVISION_3, USER_ID_1, DATA_2, "UPDATED", LEVELS_READY, getCompletedTiles(LEVELS_READY, 100), true))
                .toPact();
    }

    @Pact(consumer = "designs-query", provider = "designs-aggregate")
    public V4Pact shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequestedMessage(MessagePactBuilder builder) {
        return builder
                .given("kafka topic exists")
                .expectsToReceive("design document update requested for design " + DESIGN_ID_5 + " and 100% tiles completed and published")
                .withContent(getDesignDocumentUpdateRequestedMessage(DESIGN_ID_5, COMMAND_ID_1, new UUID(2L, 1L), REVISION_0, USER_ID_2, DATA_1, "UPDATED", LEVELS_READY, getCompletedTiles(LEVELS_READY, 100), true))
                .expectsToReceive("design document delete requested for design " + DESIGN_ID_5)
                .withContent(getDesignDocumentDeleteRequestedMessage(DESIGN_ID_5, COMMAND_ID_2, new UUID(2L, 2L), REVISION_0))
                .toPact();
    }

    @NotNull
    private static PactDslJsonBody getDesignDocumentUpdateRequestedMessage(UUID designId, UUID commandId, UUID eventId, String revision, UUID userId, String data, String status, int levels, PactDslJsonArray tiles, boolean published) {
        PactDslJsonBody event = new PactDslJsonBody()
                .uuid("designId", designId)
                .uuid("userId", userId)
                .stringMatcher("commandId", UUID6_REGEXP, commandId.toString())
                .stringMatcher("revision", REVISION_REGEXP, revision)
                .stringValue("data", data)
                .stringValue("checksum", Checksum.of(data))
                .stringValue("status", status)
                .booleanValue("published", published)
                .numberValue("levels", levels)
                .numberType("created", Instant.parse("2023-11-22T08:00:00.000Z").toEpochMilli())
                .numberType("updated", Instant.parse("2023-11-23T11:12:00.000Z").toEpochMilli())
                .object("tiles", tiles);

        PactDslJsonBody payload = new PactDslJsonBody()
                .stringMatcher("uuid", UUID6_REGEXP, eventId.toString())
                .object("data", event)
                .stringValue("type", DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .stringValue("source", MESSAGE_SOURCE);

        return new PactDslJsonBody()
                .stringValue("key", designId.toString())
                .object("value", payload);
    }

    @NotNull
    private static PactDslJsonBody getDesignDocumentDeleteRequestedMessage(UUID designId, UUID commandId, UUID eventId, String revision) {
        PactDslJsonBody event = new PactDslJsonBody()
                .uuid("designId", designId)
                .stringMatcher("commandId", UUID6_REGEXP, commandId.toString())
                .stringMatcher("revision", REVISION_REGEXP, revision);

        PactDslJsonBody payload = new PactDslJsonBody()
                .stringMatcher("uuid", UUID6_REGEXP, eventId.toString())
                .object("data", event)
                .stringValue("type", DESIGN_DOCUMENT_DELETE_REQUESTED)
                .stringValue("source", MESSAGE_SOURCE);

        return new PactDslJsonBody()
                .stringValue("key", designId.toString())
                .object("value", payload);
    }

    @NotNull
    private static PactDslJsonArray getCompletedTiles(int levels, int percentage) {
        return new PactDslJsonArray()
                .object()
                    .numberValue("level", 0)
                    .numberValue("total", 1)
                    .numberValue("completed", levels > 0 ? computeCompleted(1, percentage) : 0)
                    .closeObject()
                .object()
                    .numberValue("level", 1)
                    .numberValue("total", 4)
                    .numberValue("completed", levels > 1 ? computeCompleted(4, percentage) : 0)
                    .closeObject()
                .object()
                    .numberValue("level", 2)
                    .numberValue("total", 16)
                    .numberValue("completed", levels > 2 ? computeCompleted(16, percentage) : 0)
                    .closeObject()
                .object()
                    .numberValue("level", 3)
                    .numberValue("total", 64)
                    .numberValue("completed", levels > 3 ? computeCompleted(64, percentage) : 0)
                    .closeObject()
                .object()
                    .numberValue("level", 4)
                    .numberValue("total", 256)
                    .numberValue("completed", levels > 4 ? computeCompleted(256, percentage) : 0)
                    .closeObject()
                .object()
                    .numberValue("level", 5)
                    .numberValue("total", 1024)
                    .numberValue("completed", levels > 5 ? computeCompleted(1024, percentage) : 0)
                    .closeObject()
                .object()
                    .numberValue("level", 6)
                    .numberValue("total", 4096)
                    .numberValue("completed", levels > 6 ? computeCompleted(4096, percentage) : 0)
                    .closeObject()
                .object()
                    .numberValue("level", 7)
                    .numberValue("total", 16384)
                    .numberValue("completed", levels > 7 ? computeCompleted(16384, percentage) : 0)
                    .closeObject()
                .asArray();
    }

    @Test
    @PactTestFor(providerName = "designs-query", pactMethod = "shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequestedMessage", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should update the design after receiving a DesignDocumentUpdateRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequestedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(4);

        final OutputMessage<DesignDocumentUpdateRequested> designDocumentUpdateRequestedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()), DesignDocumentUpdateRequested.class);
        final OutputMessage<DesignDocumentUpdateRequested> designDocumentUpdateRequestedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()), DesignDocumentUpdateRequested.class);
        final OutputMessage<DesignDocumentUpdateRequested> designDocumentUpdateRequestedMessage3 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(2).asAsynchronousMessage()), DesignDocumentUpdateRequested.class);
        final OutputMessage<DesignDocumentUpdateRequested> designDocumentUpdateRequestedMessage4 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(3).asAsynchronousMessage()), DesignDocumentUpdateRequested.class);

        final List<OutputMessage<DesignDocumentUpdateRequested>> designDocumentUpdateRequestedMessages = List.of(
                designDocumentUpdateRequestedMessage1,
                designDocumentUpdateRequestedMessage2,
                designDocumentUpdateRequestedMessage3,
                designDocumentUpdateRequestedMessage4
        );

        testCases.shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequested(designDocumentUpdateRequestedMessages);
    }

    @Test
    @PactTestFor(providerName = "designs-query", pactMethod = "shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequestedMessage", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should delete the design after receiving a DesignDocumentDeleteRequested event")
    public void shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequestedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(2);

        final OutputMessage<DesignDocumentUpdateRequested> designDocumentUpdateRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()), DesignDocumentUpdateRequested.class);
        final OutputMessage<DesignDocumentDeleteRequested> designDocumentDeleteRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()), DesignDocumentDeleteRequested.class);

        testCases.shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequested(designDocumentUpdateRequestedMessage, designDocumentDeleteRequestedMessage);
    }

    private static int computeCompleted(int total, int percentage) {
        return (int) Math.ceil(total * (percentage / 100.0));
    }
}
