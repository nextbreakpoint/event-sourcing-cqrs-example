package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedOutputMapper;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.*;

@Tag("slow")
@Tag("integration")
@DisplayName("Verify behaviour of designs-event-consumer service")
public class IntegrationTests {
    private static TestCases testCases = new TestCases("IntegrationTests");

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        System.setProperty("http.port", "30122");

        testCases.before();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        testCases.after();
    }

    @Test
    @DisplayName("Should update the design after receiving a DesignInsertRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage() {
        final UUID designId = UUID.randomUUID();

        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

        testCases.shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage(designInsertRequestedMessage);
    }

    @Test
    @DisplayName("Should update the design after receiving a DesignUpdateRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage() {
        final UUID designId = UUID.randomUUID();

        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

        final DesignUpdateRequested designUpdateRequested = new DesignUpdateRequested(Uuids.timeBased(), designId, TestConstants.JSON_2, TestConstants.LEVELS);

        final OutputMessage designUpdateRequestedMessage = new DesignUpdateRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designUpdateRequested);

        testCases.shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage(designInsertRequestedMessage, designUpdateRequestedMessage);
    }

    @Test
    @DisplayName("Should update the design after receiving a DesignDeleteRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage() {
        final UUID designId = UUID.randomUUID();

        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

        final DesignDeleteRequested designDeleteRequested = new DesignDeleteRequested(Uuids.timeBased(), designId);

        final OutputMessage designDeleteRequestedMessage = new DesignDeleteRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDeleteRequested);

        testCases.shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage(designInsertRequestedMessage, designDeleteRequestedMessage);
    }

    @Test
    @DisplayName("Should update the design after receiving a TileRenderCompleted event")
    public void shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage() {
        final UUID designId = UUID.randomUUID();

        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

        final TileRenderCompleted tileRenderCompleted1 = new TileRenderCompleted(Uuids.timeBased(), designId, 0, TestConstants.CHECKSUM_1, 0, 0, 0, "FAILED");
        final TileRenderCompleted tileRenderCompleted2 = new TileRenderCompleted(Uuids.timeBased(), designId, 0, TestConstants.CHECKSUM_1, 1, 0, 0, "COMPLETED");
        final TileRenderCompleted tileRenderCompleted3 = new TileRenderCompleted(Uuids.timeBased(), designId, 0, TestConstants.CHECKSUM_1, 1, 1, 0, "COMPLETED");
        final TileRenderCompleted tileRenderCompleted4 = new TileRenderCompleted(Uuids.timeBased(), designId, 0, TestConstants.CHECKSUM_1, 2, 2, 1, "COMPLETED");
        final TileRenderCompleted tileRenderCompleted5 = new TileRenderCompleted(Uuids.timeBased(), designId, 0, TestConstants.CHECKSUM_1, 2, 3, 1, "FAILED");

        final OutputMessage tileRenderCompletedMessage1 = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(tileRenderCompleted1);
        final OutputMessage tileRenderCompletedMessage2 = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(tileRenderCompleted2);
        final OutputMessage tileRenderCompletedMessage3 = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(tileRenderCompleted3);
        final OutputMessage tileRenderCompletedMessage4 = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(tileRenderCompleted4);
        final OutputMessage tileRenderCompletedMessage5 = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(tileRenderCompleted5);

        final List<OutputMessage> tileRenderCompletedMessages = List.of(tileRenderCompletedMessage1, tileRenderCompletedMessage2, tileRenderCompletedMessage3, tileRenderCompletedMessage4, tileRenderCompletedMessage5);

        testCases.shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage(designInsertRequestedMessage, tileRenderCompletedMessages);
    }
}
