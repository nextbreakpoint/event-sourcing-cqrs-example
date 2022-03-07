package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.TimeUUID;
import com.nextbreakpoint.blueprint.common.core.Tracing;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedOutputMapper;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of designs-render service")
public class IntegrationTests {
    private static TestCases testCases = new TestCases("IntegrationTests");

    @BeforeAll
    public static void before() {
        testCases.before();
    }

    @AfterAll
    public static void after() {
        testCases.after();
    }

    @Test
    @DisplayName("Should start rendering an image after receiving a TileRenderRequested event")
    public void shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage() {
        final UUID designId1 = UUID.randomUUID();
        final UUID designId2 = UUID.randomUUID();
        final UUID designId3 = UUID.randomUUID();
        final UUID designId4 = UUID.randomUUID();
        final UUID designId5 = UUID.randomUUID();

        final TileRenderRequested tileRenderRequested1 = new TileRenderRequested(designId1, TestConstants.REVISION_0, Checksum.of(TestConstants.JSON_1), TestConstants.JSON_1, 0, 0, 0);

        final OutputMessage tileRenderRequestedMessage1 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(Tracing.of(UUID.randomUUID()), tileRenderRequested1);

        final TileRenderRequested tileRenderRequested2 = new TileRenderRequested(designId2, TestConstants.REVISION_1, Checksum.of(TestConstants.JSON_2), TestConstants.JSON_2, 4, 1, 2);

        final OutputMessage tileRenderRequestedMessage2 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(Tracing.of(UUID.randomUUID()), tileRenderRequested2);

        final TileRenderRequested tileRenderRequested3 = new TileRenderRequested(designId3, TestConstants.REVISION_1, Checksum.of(TestConstants.JSON_2), TestConstants.JSON_2, 5, 1, 2);

        final OutputMessage tileRenderRequestedMessage3 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(Tracing.of(UUID.randomUUID()), tileRenderRequested3);

        final TileRenderRequested tileRenderRequested4 = new TileRenderRequested(designId4, TestConstants.REVISION_1, Checksum.of(TestConstants.JSON_2), TestConstants.JSON_2, 6, 1, 2);

        final OutputMessage tileRenderRequestedMessage4 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(Tracing.of(UUID.randomUUID()), tileRenderRequested4);

        final TileRenderRequested tileRenderRequested5 = new TileRenderRequested(designId5, TestConstants.REVISION_1, Checksum.of(TestConstants.JSON_2), TestConstants.JSON_2, 7, 1, 2);

        final OutputMessage tileRenderRequestedMessage5 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(Tracing.of(UUID.randomUUID()), tileRenderRequested5);

        final List<OutputMessage> messages = List.of(tileRenderRequestedMessage1, tileRenderRequestedMessage2, tileRenderRequestedMessage3, tileRenderRequestedMessage4, tileRenderRequestedMessage5);

        testCases.shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(messages);
    }
}