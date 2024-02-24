package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
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
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_REGEXP;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.UUID6_REGEXP;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("docker")
@Tag("pact")
@DisplayName("Test designs-render pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
    private static TestCases testCases = new TestCases("DesignsRenderPactConsumerTests");

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

    @Pact(consumer = "designs-render")
    public V4Pact shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(MessagePactBuilder builder) {
        return builder.given("kafka topic exists")
                .expectsToReceive("tile render requested for tile 0/00000000 of design " + DESIGN_ID_1 + " with checksum 1")
                .withContent(getTileRenderRequestedMessage(DESIGN_ID_1, COMMAND_ID_1, new UUID(2L, 1L), DATA_1, REVISION_0, 0, 0, 0))
                .expectsToReceive("tile render requested for tile 4/00010002 of design " + DESIGN_ID_2 + " with checksum 2")
                .withContent(getTileRenderRequestedMessage(DESIGN_ID_2, COMMAND_ID_2, new UUID(2L, 2L), DATA_2, REVISION_1, 4, 1, 2))
                .expectsToReceive("tile render requested for tile 5/00010002 of design " + DESIGN_ID_2 + " with checksum 2")
                .withContent(getTileRenderRequestedMessage(DESIGN_ID_2, COMMAND_ID_2, new UUID(2L, 3L), DATA_2, REVISION_1, 5, 1, 2))
                .expectsToReceive("tile render requested for tile 6/00010002 of design " + DESIGN_ID_2 + " with checksum 2")
                .withContent(getTileRenderRequestedMessage(DESIGN_ID_2, COMMAND_ID_2, new UUID(2L, 4L), DATA_2, REVISION_1, 6, 1, 2))
                .expectsToReceive("tile render requested for tile 7/00010002 of design " + DESIGN_ID_2 + " with checksum 2")
                .withContent(getTileRenderRequestedMessage(DESIGN_ID_2, COMMAND_ID_2, new UUID(2L, 5L), DATA_2, REVISION_1, 7, 1, 2))
                .toPact();
    }

    @NotNull
    private static PactDslJsonBody getTileRenderRequestedMessage(UUID designId, UUID commandId, UUID eventId, String data, String revision, int level, int row, int col) {
        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", designId)
                .stringMatcher("commandId", UUID6_REGEXP, commandId.toString())
                .stringMatcher("revision", REVISION_REGEXP, revision)
                .stringValue("data", data)
                .stringValue("checksum", Checksum.of(data))
                .numberValue("level", level)
                .numberValue("row", row)
                .numberValue("col", col);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", UUID6_REGEXP, eventId.toString())
                .object("data", event1)
                .stringValue("type", TILE_RENDER_REQUESTED)
                .stringValue("source", MESSAGE_SOURCE);

        return new PactDslJsonBody()
                .stringValue("key", String.format("%s/%s/%d/%04d%04d", designId, commandId, level, row, col))
                .object("value", payload1);
    }

    @Test
    @PactTestFor(providerName = "designs-aggregate", pactMethod = "shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should start rendering an image after receiving a TileRenderRequested message")
    public void shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(5);

        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()), TileRenderRequested.class);
        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()), TileRenderRequested.class);
        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage3 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(2).asAsynchronousMessage()), TileRenderRequested.class);
        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage4 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(3).asAsynchronousMessage()), TileRenderRequested.class);
        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage5 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(4).asAsynchronousMessage()), TileRenderRequested.class);

        final List<OutputMessage<TileRenderRequested>> tileRenderRequestedMessages = List.of(
                tileRenderRequestedMessage1,
                tileRenderRequestedMessage2,
                tileRenderRequestedMessage3,
                tileRenderRequestedMessage4,
                tileRenderRequestedMessage5
        );

        testCases.shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(tileRenderRequestedMessages);
    }
}
