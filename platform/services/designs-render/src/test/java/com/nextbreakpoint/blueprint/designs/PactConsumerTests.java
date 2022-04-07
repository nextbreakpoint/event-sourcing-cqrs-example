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
@DisplayName("Test designs-render pact")
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

    @Pact(consumer = "designs-render")
    public MessagePact tileRenderRequested(MessagePactBuilder builder) {
        final UUID uuid1 = new UUID(0L, 5L);
        final UUID uuid2 = new UUID(0L, 6L);
        final UUID uuid3 = new UUID(0L, 7L);
        final UUID uuid4 = new UUID(0L, 8L);
        final UUID uuid5 = new UUID(0L, 9L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", uuid1)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP)
                .stringValue("data", TestConstants.JSON_1)
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .numberValue("level", 0)
                .numberValue("row", 0)
                .numberValue("col", 0);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.TILE_RENDER_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid1 + "/" + TestConstants.CHECKSUM_1 + "/0/00000000.png")
                .object("value", payload1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("designId", uuid2)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP)
                .stringValue("data", TestConstants.JSON_2)
                .stringValue("checksum", TestConstants.CHECKSUM_2)
                .numberValue("level", 4)
                .numberValue("row", 1)
                .numberValue("col", 2);

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.TILE_RENDER_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid2 + "/" + TestConstants.CHECKSUM_2 + "/4/00010002.png")
                .object("value", payload2);

        PactDslJsonBody event3 = new PactDslJsonBody()
                .uuid("designId", uuid3)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP)
                .stringValue("data", TestConstants.JSON_2)
                .stringValue("checksum", TestConstants.CHECKSUM_2)
                .numberValue("level", 5)
                .numberValue("row", 1)
                .numberValue("col", 2);

        PactDslJsonBody payload3 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event3)
                .stringValue("type", TestConstants.TILE_RENDER_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message3 = new PactDslJsonBody()
                .stringValue("key", uuid3 + "/" + TestConstants.CHECKSUM_2 + "/5/00010002.png")
                .object("value", payload3);

        PactDslJsonBody event4 = new PactDslJsonBody()
                .uuid("designId", uuid4)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP)
                .stringValue("data", TestConstants.JSON_2)
                .stringValue("checksum", TestConstants.CHECKSUM_2)
                .numberValue("level", 6)
                .numberValue("row", 1)
                .numberValue("col", 2);

        PactDslJsonBody payload4 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event4)
                .stringValue("type", TestConstants.TILE_RENDER_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message4 = new PactDslJsonBody()
                .stringValue("key", uuid4 + "/" + TestConstants.CHECKSUM_2 + "/6/00010002.png")
                .object("value", payload4);

        PactDslJsonBody event5 = new PactDslJsonBody()
                .uuid("designId", uuid5)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP)
                .stringValue("data", TestConstants.JSON_2)
                .stringValue("checksum", TestConstants.CHECKSUM_2)
                .numberValue("level", 7)
                .numberValue("row", 1)
                .numberValue("col", 2);

        PactDslJsonBody payload5 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event5)
                .stringValue("type", TestConstants.TILE_RENDER_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message5 = new PactDslJsonBody()
                .stringValue("key", uuid5 + "/" + TestConstants.CHECKSUM_2 + "/7/00010002.png")
                .object("value", payload5);

        return builder.given("kafka topic exists")
                .expectsToReceive("tile render requested for tile 0/00000000.png of design 00000000-0000-0000-0000-000000000005 with checksum 1")
                .withContent(message1)
                .expectsToReceive("tile render requested for tile 4/00010002.png of design 00000000-0000-0000-0000-000000000006 with checksum 2")
                .withContent(message2)
                .expectsToReceive("tile render requested for tile 5/00010002.png of design 00000000-0000-0000-0000-000000000007 with checksum 2")
                .withContent(message3)
                .expectsToReceive("tile render requested for tile 6/00010002.png of design 00000000-0000-0000-0000-000000000008 with checksum 2")
                .withContent(message4)
                .expectsToReceive("tile render requested for tile 7/00010002.png of design 00000000-0000-0000-0000-000000000009 with checksum 2")
                .withContent(message5)
                .toPact();
    }

    @Test
    @PactTestFor(providerName = "designs-aggregate", port = "1111", pactMethod = "tileRenderRequested", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
    @DisplayName("Should start rendering an image after receiving a TileRenderRequested event")
    public void shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(MessagePact pact) {
        assertThat(pact.getMessages()).hasSize(5);

        final OutputMessage tileRenderRequestedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(0)));
        final OutputMessage tileRenderRequestedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(1)));
        final OutputMessage tileRenderRequestedMessage3 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(2)));
        final OutputMessage tileRenderRequestedMessage4 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(3)));
        final OutputMessage tileRenderRequestedMessage5 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(4)));

        final List<OutputMessage> messages = List.of(tileRenderRequestedMessage1, tileRenderRequestedMessage2, tileRenderRequestedMessage3, tileRenderRequestedMessage4, tileRenderRequestedMessage5);

        testCases.shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(messages);
    }
}
