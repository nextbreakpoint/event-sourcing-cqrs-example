package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import org.assertj.core.api.SoftAssertions;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_AGGREGATE_UPDATED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_DELETED_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_REQUESTED;

public class TestAssertions {
    private TestAssertions() {}

    public static void assertExpectedMessage(Row row, OutputMessage message) {
        final String actualType = row.getString("MESSAGE_TYPE");
        final String actualValue = row.getString("MESSAGE_VALUE");
        final UUID actualUuid = row.getUuid("MESSAGE_UUID");
        final String actualToken = row.getString("MESSAGE_TOKEN");
        final String actualSource = row.getString("MESSAGE_SOURCE");
        final String actualKey = row.getString("MESSAGE_KEY");
        final Instant actualTimestamp = row.getInstant("MESSAGE_TIMESTAMP");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualUuid).isEqualTo(message.getValue().getUuid());
        softly.assertThat(actualValue).isEqualTo(message.getValue().getData());
        softly.assertThat(actualType).isEqualTo(message.getValue().getType());
        softly.assertThat(actualSource).isEqualTo(message.getValue().getSource());
        softly.assertThat(actualKey).isEqualTo(message.getKey());
        softly.assertThat(actualTimestamp).isNotNull();
        softly.assertThat(actualToken).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedDesign(Row row, String data, String status, byte[] bitmap, int levels) {
        final String actualJson = row.getString("DESIGN_DATA");
        final String actualStatus = row.getString("DESIGN_STATUS");
        final String actualChecksum = row.getString("DESIGN_CHECKSUM");
        final int actualLevels = row.getInt("DESIGN_LEVELS");
        final ByteBuffer actualBitmap = row.getByteBuffer("DESIGN_BITMAP");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualJson).isEqualTo(data);
        softly.assertThat(actualStatus).isEqualTo(status);
        softly.assertThat(actualChecksum).isNotNull();
        softly.assertThat(actualLevels).isEqualTo(levels);
        softly.assertThat(actualBitmap.array()).isEqualTo(bitmap);
        softly.assertAll();
    }

    public static void assertExpectedDesignAggregateUpdatedMessage(InputMessage actualMessage, UUID designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualMessage.getTimestamp()).isNotNull();
        softly.assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        softly.assertThat(actualMessage.getToken()).isNotNull();
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        softly.assertThat(actualMessage.getValue().getUuid()).isNotNull();
        softly.assertThat(actualMessage.getValue().getType()).isEqualTo(DESIGN_AGGREGATE_UPDATED);
        softly.assertAll();
    }

    public static void assertExpectedDesignAggregateUpdatedEvent(DesignAggregateUpdated actualEvent, UUID designId, String data, String checksum, String status, int levels) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        softly.assertThat(actualEvent.getCommandId()).isNotNull();
        softly.assertThat(actualEvent.getUserId()).isNotNull();
        softly.assertThat(actualEvent.getRevision()).isNotNull();
        softly.assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        softly.assertThat(actualEvent.getData()).isEqualTo(data);
        softly.assertThat(actualEvent.getStatus()).isEqualTo(status);
        softly.assertThat(actualEvent.getLevels()).isEqualTo(levels);
        softly.assertThat(actualEvent.getBitmap()).isNotNull();
        softly.assertThat(actualEvent.getUpdated()).isNotNull();
        softly.assertThat(actualEvent.getCreated()).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedTileRenderRequestedMessage(InputMessage actualMessage, UUID designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualMessage.getTimestamp()).isNotNull();
        softly.assertThat(actualMessage.getKey()).startsWith(designId.toString());
        softly.assertThat(actualMessage.getToken()).isNotNull();
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        softly.assertThat(actualMessage.getValue().getUuid()).isNotNull();
        softly.assertThat(actualMessage.getValue().getType()).isEqualTo(TILE_RENDER_REQUESTED);
        softly.assertAll();
    }

    public static void assertExpectedTileRenderRequestedEvent(TileRenderRequested actualEvent, UUID designId, String data, String checksum) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualEvent.getRevision()).isNotNull();
        softly.assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        softly.assertThat(actualEvent.getCommandId()).isNotNull();
        softly.assertThat(actualEvent.getData()).isEqualTo(data);
        softly.assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        softly.assertThat(actualEvent.getLevel()).isNotNull();
        softly.assertThat(actualEvent.getRow()).isNotNull();
        softly.assertThat(actualEvent.getCol()).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedDesignDocumentUpdateRequestedMessage(InputMessage actualMessage, UUID designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualMessage.getTimestamp()).isNotNull();
        softly.assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        softly.assertThat(actualMessage.getToken()).isNotNull();
        softly.assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        softly.assertThat(actualMessage.getValue().getUuid()).isNotNull();
        softly.assertThat(actualMessage.getValue().getType()).isEqualTo(DESIGN_DOCUMENT_UPDATE_REQUESTED);
        softly.assertAll();
    }

    public static void assertExpectedDesignDocumentUpdateRequestedEvent(DesignDocumentUpdateRequested actualEvent, UUID designId, UUID userId, String data, String checksum, String status, int levels, boolean published) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualEvent.getUserId()).isEqualTo(userId);
        softly.assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        softly.assertThat(actualEvent.getCommandId()).isNotNull();
        softly.assertThat(actualEvent.getRevision()).isNotNull();
        softly.assertThat(actualEvent.getData()).isEqualTo(data);
        softly.assertThat(actualEvent.getStatus()).isEqualTo(status);
        softly.assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        softly.assertThat(actualEvent.isPublished()).isEqualTo(published);
        softly.assertThat(actualEvent.getLevels()).isEqualTo(levels);
        softly.assertThat(actualEvent.getTiles()).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedDesignDocumentDeleteRequestedMessage(InputMessage actualMessage, UUID designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualMessage.getTimestamp()).isNotNull();
        softly.assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        softly.assertThat(actualMessage.getToken()).isNotNull();
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        softly.assertThat(actualMessage.getValue().getUuid()).isNotNull();
        softly.assertThat(actualMessage.getValue().getType()).isEqualTo(DESIGN_DOCUMENT_DELETED_REQUESTED);
        softly.assertAll();
    }

    public static void assertExpectedDesignDocumentDeleteRequestedEvent(DesignDocumentDeleteRequested actualEvent, UUID designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        softly.assertThat(actualEvent.getCommandId()).isNotNull();
        softly.assertThat(actualEvent.getRevision()).isNotNull();
        softly.assertAll();
    }
}
