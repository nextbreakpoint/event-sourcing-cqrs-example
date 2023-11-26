package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileStatus;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.common.Bitmap;
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
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_2;

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

    @BeforeEach
    public void beforeEach() {
        testCases.deleteData();
        testCases.getSteps().reset();
    }

    @Test
    @DisplayName("Should update the design after receiving a DesignInsertRequested message")
    public void shouldUpdateTheDesignAfterReceivingADesignInsertRequestedMessage() {
        var designInsertRequested = DesignInsertRequested.newBuilder()
                .setUserId(USER_ID_1)
                .setDesignId(DESIGN_ID_1)
                .setCommandId(COMMAND_ID_1)
                .setData(DATA_1)
                .build();

        final OutputMessage<DesignInsertRequested> designInsertRequestedMessage = MessageFactory.<DesignInsertRequested>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), designInsertRequested);

        testCases.shouldUpdateTheDesignAfterReceivingADesignInsertRequestedMessage(designInsertRequestedMessage);
    }

    @Test
    @DisplayName("Should update the design after receiving a DesignUpdateRequested message")
    public void shouldUpdateTheDesignAfterReceivingADesignUpdateRequestedMessage() {
        var designInsertRequested = DesignInsertRequested.newBuilder()
                .setUserId(USER_ID_1)
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_1)
                .setData(DATA_1)
                .build();

        var designUpdateRequested1 = DesignUpdateRequested.newBuilder()
                .setUserId(USER_ID_1)
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_2)
                .setData(DATA_2)
                .setPublished(false)
                .build();

        var designUpdateRequested2 = DesignUpdateRequested.newBuilder()
                .setUserId(USER_ID_1)
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_3)
                .setData(DATA_2)
                .setPublished(true)
                .build();

        final OutputMessage<DesignInsertRequested> designInsertRequestedMessage = MessageFactory.<DesignInsertRequested>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), designInsertRequested);
        final OutputMessage<DesignUpdateRequested> designUpdateRequestedMessage1 = MessageFactory.<DesignUpdateRequested>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), designUpdateRequested1);
        final OutputMessage<DesignUpdateRequested> designUpdateRequestedMessage2 = MessageFactory.<DesignUpdateRequested>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), designUpdateRequested2);

        testCases.shouldUpdateTheDesignAfterReceivingADesignUpdateRequestedMessage(designInsertRequestedMessage, designUpdateRequestedMessage1, designUpdateRequestedMessage2);
    }

    @Test
    @DisplayName("Should update the design after receiving a DesignDeleteRequested message")
    public void shouldUpdateTheDesignAfterReceivingADesignDeleteRequestedMessage() {
        var designInsertRequested = DesignInsertRequested.newBuilder()
                .setUserId(USER_ID_2)
                .setDesignId(DESIGN_ID_3)
                .setCommandId(COMMAND_ID_1)
                .setData(DATA_3)
                .build();

        var designDeleteRequested = DesignDeleteRequested.newBuilder()
                .setUserId(USER_ID_2)
                .setDesignId(DESIGN_ID_3)
                .setCommandId(COMMAND_ID_2)
                .build();

        final OutputMessage<DesignInsertRequested> designInsertRequestedMessage = MessageFactory.<DesignInsertRequested>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), designInsertRequested);
        final OutputMessage<DesignDeleteRequested> designDeleteRequestedMessage = MessageFactory.<DesignDeleteRequested>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), designDeleteRequested);

        testCases.shouldUpdateTheDesignAfterReceivingADesignDeleteRequestedMessage(designInsertRequestedMessage, designDeleteRequestedMessage);
    }

    @Test
    @DisplayName("Should update the design after receiving a TileRenderCompleted message")
    public void shouldUpdateTheDesignAfterReceivingATileRenderCompletedMessage() {
        var designInsertRequested = DesignInsertRequested.newBuilder()
                .setUserId(USER_ID_2)
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_1)
                .setData(DATA_2)
                .build();

        // the tile render completed events should have same command id of design insert requested event

        var tileRenderCompleted1 = TileRenderCompleted.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_1)
                .setChecksum(Checksum.of(DATA_2))
                .setRevision(REVISION_1)
                .setStatus(TileStatus.FAILED)
                .setLevel(0)
                .setRow(0)
                .setCol(0)
                .build();

        var tileRenderCompleted2 = TileRenderCompleted.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_1)
                .setChecksum(Checksum.of(DATA_2))
                .setRevision(REVISION_1)
                .setStatus(TileStatus.COMPLETED)
                .setLevel(1)
                .setRow(0)
                .setCol(0)
                .build();

        var tileRenderCompleted3 = TileRenderCompleted.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_1)
                .setChecksum(Checksum.of(DATA_2))
                .setRevision(REVISION_1)
                .setStatus(TileStatus.COMPLETED)
                .setLevel(1)
                .setRow(1)
                .setCol(0)
                .build();

        var tileRenderCompleted4 = TileRenderCompleted.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_1)
                .setChecksum(Checksum.of(DATA_2))
                .setRevision(REVISION_1)
                .setStatus(TileStatus.COMPLETED)
                .setLevel(2)
                .setRow(2)
                .setCol(1)
                .build();

        // the tile render completed events set different command id should be discarded

        var tileRenderCompleted5 = TileRenderCompleted.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_2)
                .setChecksum(Checksum.of(DATA_2))
                .setRevision(REVISION_1)
                .setStatus(TileStatus.COMPLETED)
                .setLevel(2)
                .setRow(2)
                .setCol(2)
                .build();

        final OutputMessage<DesignInsertRequested> designInsertRequestedMessage = MessageFactory.<DesignInsertRequested>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), designInsertRequested);
        final OutputMessage<TileRenderCompleted> tileRenderCompletedMessage1 = MessageFactory.<TileRenderCompleted>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), tileRenderCompleted1);
        final OutputMessage<TileRenderCompleted> tileRenderCompletedMessage2 = MessageFactory.<TileRenderCompleted>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), tileRenderCompleted2);
        final OutputMessage<TileRenderCompleted> tileRenderCompletedMessage3 = MessageFactory.<TileRenderCompleted>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), tileRenderCompleted3);
        final OutputMessage<TileRenderCompleted> tileRenderCompletedMessage4 = MessageFactory.<TileRenderCompleted>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), tileRenderCompleted4);
        final OutputMessage<TileRenderCompleted> tileRenderCompletedMessage5 = MessageFactory.<TileRenderCompleted>of(MESSAGE_SOURCE).createOutputMessage(designInsertRequested.getDesignId().toString(), tileRenderCompleted5);

        final Bitmap bitmap = Bitmap.empty();
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
