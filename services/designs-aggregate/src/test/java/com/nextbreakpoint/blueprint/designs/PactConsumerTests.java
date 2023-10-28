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
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
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

import static com.nextbreakpoint.blueprint.designs.TestConstants.CHECKSUM_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_4;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_5;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_INSERT_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_REGEXP;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.UUID6_REGEXP;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("docker")
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

    @BeforeEach
    public void beforeEach() {
        testCases.deleteData();
        testCases.getSteps().reset();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-command")
    public V4Pact shouldUpdateTheDesignAfterReceivingADesignInsertRequestedMessage(MessagePactBuilder builder) {
        return builder
                .given("kafka topic exists")
                .expectsToReceive("design insert requested for design " + DESIGN_ID_1 + " with command " + COMMAND_ID_1)
                .withContent(getDesignInsertRequestedMessage(USER_ID_1, DESIGN_ID_1, COMMAND_ID_1, new UUID(2L, 1L), DATA_1))
                .toPact();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-command")
    public V4Pact shouldUpdateTheDesignAfterReceivingADesignUpdateRequestedMessage(MessagePactBuilder builder) {
        return builder
                .given("kafka topic exists")
                .expectsToReceive("design insert requested for design " + DESIGN_ID_1 + " with command " + COMMAND_ID_1)
                .withContent(getDesignInsertRequestedMessage(USER_ID_1, DESIGN_ID_1, COMMAND_ID_1, new UUID(2L, 1L), DATA_1))
                .expectsToReceive("design update requested for design " + DESIGN_ID_1 + " with command " + COMMAND_ID_2)
                .withContent(getDesignUpdateRequestedMessage(USER_ID_1, DESIGN_ID_1, COMMAND_ID_2, new UUID(2L, 2L), DATA_2, false))
                .expectsToReceive("design update requested for design " + DESIGN_ID_1 + " with command " + COMMAND_ID_3 + " and published true")
                .withContent(getDesignUpdateRequestedMessage(USER_ID_1, DESIGN_ID_1, COMMAND_ID_3, new UUID(2L, 3L), DATA_2, true))
                .toPact();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-command")
    public V4Pact shouldUpdateTheDesignAfterReceivingADesignDeleteRequestedMessage(MessagePactBuilder builder) {
        return builder
                .given("kafka topic exists")
                .expectsToReceive("design insert requested for design " + DESIGN_ID_1 + " with command " + COMMAND_ID_1)
                .withContent(getDesignInsertRequestedMessage(USER_ID_1, DESIGN_ID_1, COMMAND_ID_1, new UUID(2L, 1L), DATA_1))
                .expectsToReceive("design delete requested for design " + DESIGN_ID_1 + " with command " + COMMAND_ID_2)
                .withContent(getDesignDeleteRequestedMessage(USER_ID_1, DESIGN_ID_1, COMMAND_ID_2, new UUID(2L, 2L)))
                .toPact();
    }

    @Pact(consumer = "designs-aggregate", provider = "designs-render")
    public V4Pact shouldUpdateTheDesignAfterReceivingATileRenderCompletedMessage(MessagePactBuilder builder) {
        return builder
                .given("kafka topic exists")
                // the tile render completed events should have same command id of design insert requested event
                .expectsToReceive("tile render completed with status FAILED for tile 0/00000000.png of design " + DESIGN_ID_2 + " and command " + COMMAND_ID_4)
                .withContent(getTileRenderCompletedMessage(DESIGN_ID_2, COMMAND_ID_4, new UUID(2L, 1L), 0, 0, 0, "FAILED", "0000000000000000-0000000000000001", CHECKSUM_2))
                .expectsToReceive("tile render completed with status COMPLETED for tile 1/00010000.png of design " + DESIGN_ID_2 + " and command " + COMMAND_ID_4)
                .withContent(getTileRenderCompletedMessage(DESIGN_ID_2, COMMAND_ID_4, new UUID(2L, 2L), 1, 0, 0, "COMPLETED", "0000000000000000-0000000000000001", CHECKSUM_2))
                .expectsToReceive("tile render completed with status COMPLETED for tile 1/00010001.png of design " + DESIGN_ID_2 + " and command " + COMMAND_ID_4)
                .withContent(getTileRenderCompletedMessage(DESIGN_ID_2, COMMAND_ID_4, new UUID(2L, 3L), 1, 1, 0, "COMPLETED", "0000000000000000-0000000000000001", CHECKSUM_2))
                .expectsToReceive("tile render completed with status COMPLETED for tile 2/00020001.png of design " + DESIGN_ID_2 + " and command " + COMMAND_ID_4)
                .withContent(getTileRenderCompletedMessage(DESIGN_ID_2, COMMAND_ID_4, new UUID(2L, 4L), 2, 2, 1, "COMPLETED", "0000000000000000-0000000000000001", CHECKSUM_2))
                // the tile render completed events with different command id should be discarded
                .expectsToReceive("tile render completed with status COMPLETED for tile 2/00020002.png of design " + DESIGN_ID_2 + " and command " + COMMAND_ID_5)
                .withContent(getTileRenderCompletedMessage(DESIGN_ID_2, COMMAND_ID_5, new UUID(2L, 5L), 2, 2, 2, "COMPLETED", "0000000000000000-0000000000000001", CHECKSUM_2))
                .toPact();
    }

    @NotNull
    private static PactDslJsonBody getDesignInsertRequestedMessage(UUID userId, UUID designId, UUID commandId, UUID eventId, String data) {
        PactDslJsonBody event = new PactDslJsonBody()
                .uuid("designId", designId)
                .uuid("userId", userId)
                .stringMatcher("commandId", UUID6_REGEXP, commandId.toString())
                .stringValue("data", data);

        PactDslJsonBody payload = new PactDslJsonBody()
                .stringMatcher("uuid", UUID6_REGEXP, eventId.toString())
                .stringValue("type", DESIGN_INSERT_REQUESTED)
                .stringValue("source", MESSAGE_SOURCE)
                .object("data", event);

        return new PactDslJsonBody()
                .stringValue("key", designId.toString())
                .object("value", payload);
    }

    @NotNull
    private static PactDslJsonBody getDesignUpdateRequestedMessage(UUID userId, UUID designId, UUID commandId, UUID eventId, String data, boolean published) {
        PactDslJsonBody event = new PactDslJsonBody()
                .uuid("designId", designId)
                .uuid("userId", userId)
                .stringMatcher("commandId", UUID6_REGEXP, commandId.toString())
                .stringValue("data", data)
                .booleanValue("published", published);

        PactDslJsonBody payload = new PactDslJsonBody()
                .stringMatcher("uuid", UUID6_REGEXP, eventId.toString())
                .stringValue("type", DESIGN_UPDATE_REQUESTED)
                .stringValue("source", MESSAGE_SOURCE)
                .object("data", event);

        return new PactDslJsonBody()
                .stringValue("key", designId.toString())
                .object("value", payload);
    }

    @NotNull
    private static PactDslJsonBody getDesignDeleteRequestedMessage(UUID userId, UUID designId, UUID commandId, UUID eventId) {
        PactDslJsonBody event = new PactDslJsonBody()
                .uuid("designId", designId)
                .uuid("userId", userId)
                .stringMatcher("commandId", UUID6_REGEXP, commandId.toString());

        PactDslJsonBody payload = new PactDslJsonBody()
                .stringMatcher("uuid", UUID6_REGEXP, eventId.toString())
                .stringValue("type", DESIGN_DELETE_REQUESTED)
                .stringValue("source", MESSAGE_SOURCE)
                .object("data", event);

        return new PactDslJsonBody()
                .stringValue("key", designId.toString())
                .object("value", payload);
    }

    @NotNull
    private static PactDslJsonBody getTileRenderCompletedMessage(UUID designId, UUID commandId, UUID eventId, int level, int row, int col, String status, String revision, String checksum) {
        PactDslJsonBody event5 = new PactDslJsonBody()
                .uuid("designId", designId)
                .uuid("commandId", commandId)
                .stringMatcher("revision", REVISION_REGEXP, revision)
                .stringValue("checksum", checksum)
                .numberValue("level", level)
                .numberValue("row", row)
                .numberValue("col", col)
                .stringMatcher("status", "COMPLETED|FAILED", status);

        PactDslJsonBody payload5 = new PactDslJsonBody()
                .stringMatcher("uuid", UUID6_REGEXP, eventId.toString())
                .stringValue("type", TILE_RENDER_COMPLETED)
                .stringValue("source", MESSAGE_SOURCE)
                .object("data", event5);

        return new PactDslJsonBody()
                .stringValue("key", String.format("%s/%s/%d/%04d%04d.png", designId, commandId, level, row, col))
                .object("value", payload5);
    }

    @Test
    @PactTestFor(providerName = "designs-command", pactMethod = "shouldUpdateTheDesignAfterReceivingADesignInsertRequestedMessage", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should update the design after receiving a DesignInsertRequested message")
    public void shouldUpdateTheDesignAfterReceivingADesignInsertRequestedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(1);

        final OutputMessage designInsertRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()));

        testCases.shouldUpdateTheDesignAfterReceivingADesignInsertRequestedMessage(designInsertRequestedMessage);
    }

    @Test
    @PactTestFor(providerName = "designs-command", pactMethod = "shouldUpdateTheDesignAfterReceivingADesignUpdateRequestedMessage", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should update the design after receiving a DesignUpdateRequested message")
    public void shouldUpdateTheDesignAfterReceivingADesignUpdateRequestedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(3);

        final OutputMessage designInsertRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()));
        final OutputMessage designUpdateRequestedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()));
        final OutputMessage designUpdateRequestedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(2).asAsynchronousMessage()));

        testCases.shouldUpdateTheDesignAfterReceivingADesignUpdateRequestedMessage(designInsertRequestedMessage, designUpdateRequestedMessage1, designUpdateRequestedMessage2);
    }

    @Test
    @PactTestFor(providerName = "designs-command", pactMethod = "shouldUpdateTheDesignAfterReceivingADesignDeleteRequestedMessage", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should update the design after receiving a DesignDeleteRequested message")
    public void shouldUpdateTheDesignAfterReceivingADesignDeleteRequestedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(2);

        final OutputMessage designInsertRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()));
        final OutputMessage designDeleteRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()));

        testCases.shouldUpdateTheDesignAfterReceivingADesignDeleteRequestedMessage(designInsertRequestedMessage, designDeleteRequestedMessage);
    }

    @Test
    @PactTestFor(providerName = "designs-render", pactMethod = "shouldUpdateTheDesignAfterReceivingATileRenderCompletedMessage", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    @DisplayName("Should update the design after receiving a TileRenderCompleted message")
    public void shouldUpdateTheDesignAfterReceivingATileRenderCompletedMessage(V4Pact pact) {
        assertThat(pact.getInteractions()).hasSize(5);

        final DesignInsertRequestedOutputMapper outputMapper = new DesignInsertRequestedOutputMapper(MESSAGE_SOURCE);
        final OutputMessage designInsertRequestedMessage = outputMapper.transform(new DesignInsertRequested(DESIGN_ID_2, COMMAND_ID_4, USER_ID_2, DATA_2));
        final OutputMessage tileRenderCompletedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(0).asAsynchronousMessage()));
        final OutputMessage tileRenderCompletedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(1).asAsynchronousMessage()));
        final OutputMessage tileRenderCompletedMessage3 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(2).asAsynchronousMessage()));
        final OutputMessage tileRenderCompletedMessage4 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(3).asAsynchronousMessage()));
        final OutputMessage tileRenderCompletedMessage5 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getInteractions().get(4).asAsynchronousMessage()));

        final TilesBitmap bitmap = TilesBitmap.empty();
        bitmap.putTile(0, 0, 0);
        bitmap.putTile(1, 0, 0);
        bitmap.putTile(1, 1, 0);
        bitmap.putTile(2, 2, 1);

        var tileRenderCompletedMessages = List.of(
                tileRenderCompletedMessage1,
                tileRenderCompletedMessage2,
                tileRenderCompletedMessage3,
                tileRenderCompletedMessage4,
                tileRenderCompletedMessage5
        );

        testCases.shouldUpdateTheDesignAfterReceivingATileRenderCompletedMessage(designInsertRequestedMessage, tileRenderCompletedMessages, bitmap);
    }
}
