package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.TimeUUID;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedOutputMapper;
import com.nextbreakpoint.blueprint.designs.common.Render;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of designs-aggregate service")
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
    @DisplayName("Should update the design after receiving a DesignInsertRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage() {
        final UUID designId = UUID.randomUUID();

        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(designId, TestConstants.USER_ID, TimeUUID.next(), TestConstants.JSON_1);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

        testCases.shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage(designInsertRequestedMessage);
    }

    @Test
    @DisplayName("Should update the design after receiving a DesignUpdateRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage() {
        final UUID designId = UUID.randomUUID();

        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(designId, TestConstants.USER_ID, TimeUUID.next(), TestConstants.JSON_1);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

        final DesignUpdateRequested designUpdateRequested = new DesignUpdateRequested(designId, TestConstants.USER_ID, TimeUUID.next(), TestConstants.JSON_2, false);

        final OutputMessage designUpdateRequestedMessage = new DesignUpdateRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designUpdateRequested);

        testCases.shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage(designInsertRequestedMessage, designUpdateRequestedMessage);
    }

    @Test
    @DisplayName("Should update the design after receiving a DesignDeleteRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage() {
        final UUID designId = UUID.randomUUID();

        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(designId, TestConstants.USER_ID, TimeUUID.next(), TestConstants.JSON_1);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

        final DesignDeleteRequested designDeleteRequested = new DesignDeleteRequested(designId, TestConstants.USER_ID, TimeUUID.next());

        final OutputMessage designDeleteRequestedMessage = new DesignDeleteRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDeleteRequested);

        testCases.shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage(designInsertRequestedMessage, designDeleteRequestedMessage);
    }

    @Test
    @DisplayName("Should update the design after receiving a TileRenderCompleted event")
    public void shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage() {
        final UUID designId = UUID.randomUUID();

        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(designId, TestConstants.USER_ID, TimeUUID.next(), TestConstants.JSON_1);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

        final TileRenderCompleted tileRenderCompleted1 = new TileRenderCompleted(designId, TestConstants.REVISION_0, TestConstants.CHECKSUM_1, 0, 0, 0, "FAILED");
        final TileRenderCompleted tileRenderCompleted2 = new TileRenderCompleted(designId, TestConstants.REVISION_1, TestConstants.CHECKSUM_1, 1, 0, 0, "COMPLETED");
        final TileRenderCompleted tileRenderCompleted3 = new TileRenderCompleted(designId, TestConstants.REVISION_2, TestConstants.CHECKSUM_1, 1, 1, 0, "COMPLETED");
        final TileRenderCompleted tileRenderCompleted4 = new TileRenderCompleted(designId, TestConstants.REVISION_3, TestConstants.CHECKSUM_1, 2, 2, 1, "COMPLETED");
        final TileRenderCompleted tileRenderCompleted5 = new TileRenderCompleted(designId, TestConstants.REVISION_4, TestConstants.CHECKSUM_1, 2, 3, 1, "FAILED");

        final OutputMessage tileRenderCompletedMessage1 = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(tileRenderCompleted1);
        final OutputMessage tileRenderCompletedMessage2 = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(tileRenderCompleted2);
        final OutputMessage tileRenderCompletedMessage3 = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(tileRenderCompleted3);
        final OutputMessage tileRenderCompletedMessage4 = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(tileRenderCompleted4);
        final OutputMessage tileRenderCompletedMessage5 = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(tileRenderCompleted5);

        final List<OutputMessage> tileRenderCompletedMessages = List.of(tileRenderCompletedMessage1, tileRenderCompletedMessage2, tileRenderCompletedMessage3, tileRenderCompletedMessage4, tileRenderCompletedMessage5);

        testCases.shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage(designInsertRequestedMessage, tileRenderCompletedMessages);
    }
}
