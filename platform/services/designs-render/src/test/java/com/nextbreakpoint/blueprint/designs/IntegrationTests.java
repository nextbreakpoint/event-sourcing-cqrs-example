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
        final UUID designId = UUID.randomUUID();

        final TileRenderRequested tileRenderRequested1 = new TileRenderRequested(designId, TestConstants.REVISION_0, Checksum.of(TestConstants.JSON_1), TestConstants.JSON_1, 0, 0, 0);

        final OutputMessage tileRenderRequestedMessage1 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createBucketKey).transform(Tracing.of(UUID.randomUUID()), tileRenderRequested1);

        final TileRenderRequested tileRenderRequested2 = new TileRenderRequested(designId, TestConstants.REVISION_1, Checksum.of(TestConstants.JSON_2), TestConstants.JSON_2, 1, 1, 2);

        final OutputMessage tileRenderRequestedMessage2 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createBucketKey).transform(Tracing.of(UUID.randomUUID()), tileRenderRequested2);

        testCases.shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(List.of(tileRenderRequestedMessage1, tileRenderRequestedMessage2));
    }
}