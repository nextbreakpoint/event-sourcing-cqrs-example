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

    @Pact(consumer = "designs-render")
    public MessagePact tileRenderRequested(MessagePactBuilder builder) {
        final UUID uuid = new UUID(0L, 5L);

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .numberType("revision")
                .stringValue("data", TestConstants.JSON_1)
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .numberValue("level", 0)
                .numberValue("row", 0)
                .numberValue("col", 0);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID1_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.TILE_RENDER_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace1 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", TestConstants.CHECKSUM_1 + "/0/00000000.png")
                .object("value", payload1)
                .object("headers", trace1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .stringMatcher("eventId", TestConstants.UUID1_REGEXP)
                .numberType("revision")
                .stringValue("data", TestConstants.JSON_2)
                .stringValue("checksum", TestConstants.CHECKSUM_2)
                .numberValue("level", 1)
                .numberValue("row", 1)
                .numberValue("col", 2);

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID1_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.TILE_RENDER_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody trace2 = new PactDslJsonBody()
                .stringMatcher("X-TRACE-TRACE-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-SPAN-ID", TestConstants.UUID6_REGEXP)
                .stringMatcher("X-TRACE-PARENT", TestConstants.UUID6_REGEXP);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", TestConstants.CHECKSUM_2 + "/1/00010002.png")
                .object("value", payload2)
                .object("headers", trace2);

        return builder.given("kafka topic exists")
                .expectsToReceive("tile render requested for tile 0/00000000.png of design 00000000-0000-0000-0000-000000000004 with checksum 1")
                .withContent(message1)
                .expectsToReceive("tile render requested for tile 1/00010002.png of design 00000000-0000-0000-0000-000000000004 with checksum 2")
                .withContent(message2)
                .toPact();
    }

    @Test
    @PactTestFor(providerName = "designs-aggregate", port = "1111", pactMethod = "tileRenderRequested", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
    @DisplayName("Should start rendering an image after receiving a TileRenderRequested event")
    public void shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(MessagePact pact) {
        assertThat(pact.getMessages()).hasSize(2);

        final OutputMessage tileRenderRequestedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(0)));
        final OutputMessage tileRenderRequestedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(1)));

        testCases.shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(List.of(tileRenderRequestedMessage1, tileRenderRequestedMessage2));
    }
}
