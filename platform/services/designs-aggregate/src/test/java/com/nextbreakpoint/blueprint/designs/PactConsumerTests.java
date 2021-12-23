package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

@Tag("slow")
@Tag("pact")
@DisplayName("Test designs-aggregate pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
    private static TestCases testCases = new TestCases("PactTests");

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

    @Pact(consumer = "designs-aggregate", provider = "designs-command")
    public MessagePact designInsertRequested(MessagePactBuilder builder) {
        final UUID uuid = new UUID(0L, 1L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("uuid", uuid)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .stringValue("data", TestConstants.JSON_1)
                .numberValue("levels", TestConstants.LEVELS);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload1);

        return builder
                .given("kafka topic exists")
                .expectsToReceive("design insert requested for design 00000000-0000-0000-0000-000000000001")
                .withContent(message1)
                .toPact();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-command")
    public MessagePact designUpdateRequested(MessagePactBuilder builder) {
        final UUID uuid = new UUID(0L, 2L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("uuid", uuid)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .stringValue("data", TestConstants.JSON_1)
                .numberValue("levels", TestConstants.LEVELS);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("uuid", uuid)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .stringValue("data", TestConstants.JSON_2)
                .numberValue("levels", TestConstants.LEVELS);

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.DESIGN_UPDATE_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload2);

        return builder
                .given("kafka topic exists")
                .expectsToReceive("design insert requested for design 00000000-0000-0000-0000-000000000002")
                .withContent(message1)
                .expectsToReceive("design update requested for design 00000000-0000-0000-0000-000000000002")
                .withContent(message2)
                .toPact();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-command")
    public MessagePact designDeleteRequested(MessagePactBuilder builder) {
        final UUID uuid = new UUID(0L, 3L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("uuid", uuid)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .stringValue("data", TestConstants.JSON_1)
                .numberValue("levels", TestConstants.LEVELS);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("uuid", uuid)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP);

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.DESIGN_DELETE_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload2);

        return builder
                .given("kafka topic exists")
                .expectsToReceive("design insert requested for design 00000000-0000-0000-0000-000000000003")
                .withContent(message1)
                .expectsToReceive("design delete requested for design 00000000-0000-0000-0000-000000000003")
                .withContent(message2)
                .toPact();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-render")
    public MessagePact tileRenderCompleted(MessagePactBuilder builder) {
        final UUID uuid = new UUID(0L, 4L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("uuid", uuid)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .numberType("esid")
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

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("uuid", uuid)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .numberType("esid")
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

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload2);

        PactDslJsonBody event3 = new PactDslJsonBody()
                .uuid("uuid", uuid)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .numberType("esid")
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

        PactDslJsonBody message3 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload3);

        PactDslJsonBody event4 = new PactDslJsonBody()
                .uuid("uuid", uuid)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .numberType("esid")
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

        PactDslJsonBody message4 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload4);

        PactDslJsonBody event5 = new PactDslJsonBody()
                .uuid("uuid", uuid)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .numberType("esid")
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

        PactDslJsonBody message5 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload5);

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
    @PactTestFor(providerName = "designs-command", port = "1111", pactMethod = "designInsertRequested", providerType = ProviderType.ASYNCH)
    @DisplayName("Should update the design after receiving a DesignInsertRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage(MessagePact messagePact) {
        final OutputMessage designInsertRequestedMessage = TestUtils.toOutputMessage(messagePact.getMessages().get(0));

        testCases.shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage(designInsertRequestedMessage);
    }

    @Test
    @PactTestFor(providerName = "designs-command", port = "1112", pactMethod = "designUpdateRequested", providerType = ProviderType.ASYNCH)
    @DisplayName("Should update the design after receiving a DesignUpdateRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage(MessagePact messagePact) {
        final OutputMessage designInsertRequestedMessage = TestUtils.toOutputMessage(messagePact.getMessages().get(0));

        final OutputMessage designUpdateRequestedMessage = TestUtils.toOutputMessage(messagePact.getMessages().get(1));

        testCases.shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage(designInsertRequestedMessage, designUpdateRequestedMessage);
    }

    @Test
    @PactTestFor(providerName = "designs-command", port = "1113", pactMethod = "designDeleteRequested", providerType = ProviderType.ASYNCH)
    @DisplayName("Should update the design after receiving a DesignDeleteRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage(MessagePact messagePact) {
        final OutputMessage designInsertRequestedMessage = TestUtils.toOutputMessage(messagePact.getMessages().get(0));

        final OutputMessage designDeleteRequestedMessage = TestUtils.toOutputMessage(messagePact.getMessages().get(1));

        testCases.shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage(designInsertRequestedMessage, designDeleteRequestedMessage);
    }

    @Test
    @PactTestFor(providerName = "designs-render", port = "1114", pactMethod = "tileRenderCompleted", providerType = ProviderType.ASYNCH)
    @DisplayName("Should update the design after receiving a TileRenderCompleted event")
    public void shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage(MessagePact messagePact) {
        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), new UUID(0L, 4L), TestConstants.JSON_1, TestConstants.LEVELS);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

        final OutputMessage tileRenderCompletedMessage1 = TestUtils.toOutputMessage(messagePact.getMessages().get(0));

        final OutputMessage tileRenderCompletedMessage2 = TestUtils.toOutputMessage(messagePact.getMessages().get(1));

        final OutputMessage tileRenderCompletedMessage3 = TestUtils.toOutputMessage(messagePact.getMessages().get(2));

        final OutputMessage tileRenderCompletedMessage4 = TestUtils.toOutputMessage(messagePact.getMessages().get(3));

        final OutputMessage tileRenderCompletedMessage5 = TestUtils.toOutputMessage(messagePact.getMessages().get(4));

        final List<OutputMessage> tileRenderCompletedMessages = List.of(tileRenderCompletedMessage1, tileRenderCompletedMessage2, tileRenderCompletedMessage3, tileRenderCompletedMessage4, tileRenderCompletedMessage5);

        testCases.shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage(designInsertRequestedMessage, tileRenderCompletedMessages);
    }
}
