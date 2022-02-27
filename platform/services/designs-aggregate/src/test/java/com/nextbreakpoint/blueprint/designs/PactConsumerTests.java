package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tracing;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Tag("pact")
@DisplayName("Test designs-aggregate pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
    private static TestCases testCases = new TestCases("PactTests");

    @BeforeAll
    public static void before() {
        System.setProperty("pact_do_not_track", "true");

        testCases.before();

        System.setProperty("pact.showStacktrace", "true");
        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.provider.version", testCases.getVersion());
    }

    @AfterAll
    public static void after() {
        testCases.after();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-command")
    public V4Pact designInsertRequested(MessagePactBuilder builder) {
        final UUID uuid = new UUID(0L, 1L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .uuid("userId", TestConstants.USER_ID)
                .stringMatcher("changeId", TestConstants.UUID6_REGEXP)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .stringValue("data", TestConstants.JSON_1)
                .numberValue("levels", TestConstants.LEVELS);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace1 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload1)
                .object("headers", trace1);

        return builder
                .given("kafka topic exists")
                .expectsToReceive("design insert requested for design 00000000-0000-0000-0000-000000000001")
                .withContent(message1)
                .toPact();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-command")
    public V4Pact designUpdateRequested(MessagePactBuilder builder) {
        final UUID uuid = new UUID(0L, 2L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .uuid("userId", TestConstants.USER_ID)
                .stringMatcher("changeId", TestConstants.UUID6_REGEXP)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .stringValue("data", TestConstants.JSON_1)
                .numberValue("levels", TestConstants.LEVELS);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace1 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload1)
                .object("headers", trace1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .uuid("userId", TestConstants.USER_ID)
                .stringMatcher("changeId", TestConstants.UUID6_REGEXP)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .stringValue("data", TestConstants.JSON_2)
                .numberValue("levels", TestConstants.LEVELS);

        PactDslJsonBody trace2 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.DESIGN_UPDATE_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload2)
                .object("headers", trace2);

        return builder
                .given("kafka topic exists")
                .expectsToReceive("design insert requested for design 00000000-0000-0000-0000-000000000002")
                .withContent(message1)
                .expectsToReceive("design update requested for design 00000000-0000-0000-0000-000000000002")
                .withContent(message2)
                .toPact();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-command")
    public V4Pact designDeleteRequested(MessagePactBuilder builder) {
        final UUID uuid = new UUID(0L, 3L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .uuid("userId", TestConstants.USER_ID)
                .stringMatcher("changeId", TestConstants.UUID6_REGEXP)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .stringValue("data", TestConstants.JSON_1)
                .numberValue("levels", TestConstants.LEVELS);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace1 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload1)
                .object("headers", trace1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .uuid("userId", TestConstants.USER_ID)
                .stringMatcher("changeId", TestConstants.UUID6_REGEXP)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP);

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.DESIGN_DELETE_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace2 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload2)
                .object("headers", trace2);

        return builder
                .given("kafka topic exists")
                .expectsToReceive("design insert requested for design 00000000-0000-0000-0000-000000000003")
                .withContent(message1)
                .expectsToReceive("design delete requested for design 00000000-0000-0000-0000-000000000003")
                .withContent(message2)
                .toPact();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-render")
    public V4Pact tileRenderCompleted(MessagePactBuilder builder) {
        final UUID uuid = new UUID(0L, 4L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .numberType("revision")
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .numberValue("level", 0)
                .numberValue("row", 0)
                .numberValue("col", 0)
                .stringValue("status", "FAILED");

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace1 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload1)
                .object("headers", trace1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .numberType("revision")
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .numberValue("level", 1)
                .numberValue("row", 0)
                .numberValue("col", 0)
                .stringValue("status", "COMPLETED");

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace2 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload2)
                .object("headers", trace2);

        PactDslJsonBody event3 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .numberType("revision")
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .numberValue("level", 1)
                .numberValue("row", 1)
                .numberValue("col", 0)
                .stringValue("status", "COMPLETED");

        PactDslJsonBody payload3 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event3)
                .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace3 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message3 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload3)
                .object("headers", trace3);

        PactDslJsonBody event4 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .numberType("revision")
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .numberValue("level", 2)
                .numberValue("row", 2)
                .numberValue("col", 1)
                .stringValue("status", "COMPLETED");

        PactDslJsonBody payload4 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event4)
                .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace4 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message4 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload4)
                .object("headers", trace4);

        PactDslJsonBody event5 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .numberType("revision")
                .stringValue("checksum", TestConstants.CHECKSUM_2)
                .numberValue("level", 2)
                .numberValue("row", 3)
                .numberValue("col", 1)
                .stringValue("status", "FAILED");

        PactDslJsonBody payload5 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event5)
                .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace5 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message5 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload5)
                .object("headers", trace5);

        return builder.given("kafka topic exists")
                .expectsToReceive("tile render completed with status FAILED for tile 0/00000000.png of design 00000000-0000-0000-0000-000000000004 with checksum 1")
                .withContent(message1)
                .expectsToReceive("tile render completed with status COMPLETED for tile 1/00010000.png of design 00000000-0000-0000-0000-000000000004 with checksum 1")
                .withContent(message2)
                .expectsToReceive("tile render completed with status COMPLETED for tile 1/00010001.png of design 00000000-0000-0000-0000-000000000004 with checksum 1")
                .withContent(message3)
                .expectsToReceive("tile render completed with status COMPLETED for tile 2/00020001.png of design 00000000-0000-0000-0000-000000000004 with checksum 1")
                .withContent(message4)
                .expectsToReceive("tile render completed with status FAILED for tile 2/00030001.png of design 00000000-0000-0000-0000-000000000004 with checksum 2")
                .withContent(message5)
                .toPact();
    }

    @Test
    @PactTestFor(providerName = "designs-command", port = "1111", pactMethod = "designInsertRequested", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should update the design after receiving a DesignInsertRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(1);

        final OutputMessage designInsertRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()));

        testCases.shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage(designInsertRequestedMessage);
    }

    @Test
    @PactTestFor(providerName = "designs-command", port = "1112", pactMethod = "designUpdateRequested", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should update the design after receiving a DesignUpdateRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(2);

        final OutputMessage designInsertRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()));
        final OutputMessage designUpdateRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()));

        testCases.shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage(designInsertRequestedMessage, designUpdateRequestedMessage);
    }

    @Test
    @PactTestFor(providerName = "designs-command", port = "1113", pactMethod = "designDeleteRequested", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should update the design after receiving a DesignDeleteRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(2);

        final OutputMessage designInsertRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()));
        final OutputMessage designDeleteRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()));

        testCases.shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage(designInsertRequestedMessage, designDeleteRequestedMessage);
    }

    @Test
    @PactTestFor(providerName = "designs-render", port = "1114", pactMethod = "tileRenderCompleted", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should update the design after receiving a TileRenderCompleted event")
    public void shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(5);

        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(TestConstants.USER_ID, Uuids.timeBased(), new UUID(0L, 4L), UUID.randomUUID(), TestConstants.JSON_1, TestConstants.LEVELS);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(Tracing.of(UUID.randomUUID()), designInsertRequested);

        final OutputMessage tileRenderCompletedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()));
        final OutputMessage tileRenderCompletedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()));
        final OutputMessage tileRenderCompletedMessage3 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(2).asAsynchronousMessage()));
        final OutputMessage tileRenderCompletedMessage4 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(3).asAsynchronousMessage()));
        final OutputMessage tileRenderCompletedMessage5 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(4).asAsynchronousMessage()));

        final List<OutputMessage> tileRenderCompletedMessages = List.of(tileRenderCompletedMessage1, tileRenderCompletedMessage2, tileRenderCompletedMessage3, tileRenderCompletedMessage4, tileRenderCompletedMessage5);

        testCases.shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage(designInsertRequestedMessage, tileRenderCompletedMessages);
    }
}
