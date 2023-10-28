package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedOutputMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_2;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of designs-aggregate service")
public class IntegrationTests {
    private static TestCases testCases = new TestCases("IntegrationTests");

    private final DesignInsertRequestedOutputMapper designInsertRequestedMapper = new DesignInsertRequestedOutputMapper(MESSAGE_SOURCE);
    private final DesignUpdateRequestedOutputMapper designUpdateRequestedMapper = new DesignUpdateRequestedOutputMapper(MESSAGE_SOURCE);
    private final DesignDeleteRequestedOutputMapper designDeleteRequestedMapper = new DesignDeleteRequestedOutputMapper(MESSAGE_SOURCE);
    private final TileRenderCompletedOutputMapper tileRenderCompletedMapper = new TileRenderCompletedOutputMapper(MESSAGE_SOURCE);

    @BeforeAll
    public static void before() {
        testCases.before();
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

    @Test
    @DisplayName("Should update the design after receiving a DesignInsertRequested message")
    public void shouldUpdateTheDesignAfterReceivingADesignInsertRequestedMessage() {
        var designInsertRequested = DesignInsertRequested.builder()
                .withUserId(USER_ID_1)
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withData(DATA_1)
                .build();

        final OutputMessage designInsertRequestedMessage = designInsertRequestedMapper.transform(designInsertRequested);

        testCases.shouldUpdateTheDesignAfterReceivingADesignInsertRequestedMessage(designInsertRequestedMessage);
    }

    @Test
    @DisplayName("Should update the design after receiving a DesignUpdateRequested message")
    public void shouldUpdateTheDesignAfterReceivingADesignUpdateRequestedMessage() {
        var designInsertRequested = DesignInsertRequested.builder()
                .withUserId(USER_ID_1)
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withData(DATA_1)
                .build();

        var designUpdateRequested1 = DesignUpdateRequested.builder()
                .withUserId(USER_ID_1)
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_2)
                .withData(DATA_2)
                .withPublished(false)
                .build();

        var designUpdateRequested2 = DesignUpdateRequested.builder()
                .withUserId(USER_ID_1)
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_3)
                .withData(DATA_2)
                .withPublished(true)
                .build();

        final OutputMessage designInsertRequestedMessage = designInsertRequestedMapper.transform(designInsertRequested);
        final OutputMessage designUpdateRequestedMessage1 = designUpdateRequestedMapper.transform(designUpdateRequested1);
        final OutputMessage designUpdateRequestedMessage2 = designUpdateRequestedMapper.transform(designUpdateRequested2);

        testCases.shouldUpdateTheDesignAfterReceivingADesignUpdateRequestedMessage(designInsertRequestedMessage, designUpdateRequestedMessage1, designUpdateRequestedMessage2);
    }

    @Test
    @DisplayName("Should update the design after receiving a DesignDeleteRequested message")
    public void shouldUpdateTheDesignAfterReceivingADesignDeleteRequestedMessage() {
        var designInsertRequested = DesignInsertRequested.builder()
                .withUserId(USER_ID_1)
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withData(DATA_1)
                .build();

        var designDeleteRequested = DesignDeleteRequested.builder()
                .withUserId(USER_ID_1)
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_3)
                .build();

        final OutputMessage designInsertRequestedMessage = designInsertRequestedMapper.transform(designInsertRequested);
        final OutputMessage designDeleteRequestedMessage = designDeleteRequestedMapper.transform(designDeleteRequested);

        testCases.shouldUpdateTheDesignAfterReceivingADesignDeleteRequestedMessage(designInsertRequestedMessage, designDeleteRequestedMessage);
    }

    @Test
    @DisplayName("Should update the design after receiving a TileRenderCompleted message")
    public void shouldUpdateTheDesignAfterReceivingATileRenderCompletedMessage() {
        var designInsertRequested = DesignInsertRequested.builder()
                .withUserId(USER_ID_2)
                .withDesignId(DESIGN_ID_2)
                .withCommandId(COMMAND_ID_1)
                .withData(DATA_2)
                .build();

        // the tile render completed events should have same command id of design insert requested event

        var tileRenderCompleted1 = TileRenderCompleted.builder()
                .withDesignId(DESIGN_ID_2)
                .withCommandId(COMMAND_ID_1)
                .withChecksum(Checksum.of(DATA_2))
                .withRevision(REVISION_1)
                .withStatus("FAILED")
                .withLevel(0)
                .withRow(0)
                .withCol(0)
                .build();

        var tileRenderCompleted2 = TileRenderCompleted.builder()
                .withDesignId(DESIGN_ID_2)
                .withCommandId(COMMAND_ID_1)
                .withChecksum(Checksum.of(DATA_2))
                .withRevision(REVISION_1)
                .withStatus("COMPLETED")
                .withLevel(1)
                .withRow(0)
                .withCol(0)
                .build();

        var tileRenderCompleted3 = TileRenderCompleted.builder()
                .withDesignId(DESIGN_ID_2)
                .withCommandId(COMMAND_ID_1)
                .withChecksum(Checksum.of(DATA_2))
                .withRevision(REVISION_1)
                .withStatus("COMPLETED")
                .withLevel(1)
                .withRow(1)
                .withCol(0)
                .build();

        var tileRenderCompleted4 = TileRenderCompleted.builder()
                .withDesignId(DESIGN_ID_2)
                .withCommandId(COMMAND_ID_1)
                .withChecksum(Checksum.of(DATA_2))
                .withRevision(REVISION_1)
                .withStatus("COMPLETED")
                .withLevel(2)
                .withRow(2)
                .withCol(1)
                .build();

        // the tile render completed events with different command id should be discarded

        var tileRenderCompleted5 = TileRenderCompleted.builder()
                .withDesignId(DESIGN_ID_2)
                .withCommandId(COMMAND_ID_2)
                .withChecksum(Checksum.of(DATA_2))
                .withRevision(REVISION_1)
                .withStatus("COMPLETED")
                .withLevel(2)
                .withRow(2)
                .withCol(2)
                .build();

        final OutputMessage designInsertRequestedMessage = designInsertRequestedMapper.transform(designInsertRequested);
        final OutputMessage tileRenderCompletedMessage1 = tileRenderCompletedMapper.transform(tileRenderCompleted1);
        final OutputMessage tileRenderCompletedMessage2 = tileRenderCompletedMapper.transform(tileRenderCompleted2);
        final OutputMessage tileRenderCompletedMessage3 = tileRenderCompletedMapper.transform(tileRenderCompleted3);
        final OutputMessage tileRenderCompletedMessage4 = tileRenderCompletedMapper.transform(tileRenderCompleted4);
        final OutputMessage tileRenderCompletedMessage5 = tileRenderCompletedMapper.transform(tileRenderCompleted5);

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
