package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.*;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(actualUuid).isEqualTo(message.getValue().getUuid());
        assertThat(actualValue).isEqualTo(message.getValue().getData());
        assertThat(actualType).isEqualTo(message.getValue().getType());
        assertThat(actualSource).isEqualTo(message.getValue().getSource());
        assertThat(actualKey).isEqualTo(message.getKey());
        assertThat(actualTimestamp).isNotNull();
        assertThat(actualToken).isNotNull();
    }

    public static void assertExpectedDesign(Row row, String data, String status, byte[] bitmap) {
        final String actualJson = row.getString("DESIGN_DATA");
        final String actualStatus = row.getString("DESIGN_STATUS");
        final String actualChecksum = row.getString("DESIGN_CHECKSUM");
        final int actualLevels = row.getInt("DESIGN_LEVELS");
        final ByteBuffer actualBitmap = row.getByteBuffer("DESIGN_BITMAP");
        assertThat(actualJson).isEqualTo(data);
        assertThat(actualStatus).isEqualTo(status);
        assertThat(actualChecksum).isNotNull();
        assertThat(actualLevels).isEqualTo(TestConstants.LEVELS);
        assertThat(actualBitmap.array()).isEqualTo(bitmap);
    }

    public static void assertExpectedDesignAggregateUpdateRequestedMessage(InputMessage actualMessage, UUID designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_AGGREGATE_UPDATE_REQUESTED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        DesignAggregateUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignAggregateUpdateRequested.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
    }

    public static void assertExpectedDesignAggregateUpdateCompletedMessage(InputMessage actualMessage, UUID designId, String data, String checksum, String status) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_AGGREGATE_UPDATE_COMPLETED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        DesignAggregateUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignAggregateUpdateCompleted.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getStatus()).isEqualTo(status);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getLevels()).isEqualTo(3);
    }

    public static void assertExpectedTileRenderRequestedMessage(InputMessage actualMessage, String partitionKey) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(partitionKey);
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_RENDER_REQUESTED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
    }

    public static void assertExpectedTileRenderRequestedEvent(TileRenderRequested actualEvent, UUID designId, String data, String checksum) {
        assertThat(actualEvent.getRevision()).isNotNull();
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getLevel()).isNotNull();
        assertThat(actualEvent.getRow()).isNotNull();
        assertThat(actualEvent.getCol()).isNotNull();
    }

    public static void assertExpectedTileRenderCompletedMessage(InputMessage actualMessage, UUID designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_RENDER_COMPLETED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
    }

    public static void assertExpectedTileRenderCompletedEvent(TileRenderCompleted actualEvent, UUID designId, String checksum, String status) {
        assertThat(actualEvent.getRevision()).isNotNull();
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getStatus()).isEqualTo(status);
        assertThat(actualEvent.getLevel()).isNotNull();
        assertThat(actualEvent.getRow()).isNotNull();
        assertThat(actualEvent.getCol()).isNotNull();
    }

    public static void assertExpectedTileAggregateUpdateRequiredMessage(InputMessage actualMessage, UUID designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_AGGREGATE_UPDATE_REQUIRED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        TileAggregateUpdateRequired actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileAggregateUpdateRequired.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
    }

    public static void assertExpectedTileAggregateUpdateRequestedMessage(InputMessage actualMessage, UUID designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_AGGREGATE_UPDATE_REQUESTED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        TileAggregateUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileAggregateUpdateRequested.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
    }

    public static void assertExpectedTileAggregateUpdateCompletedMessage(InputMessage actualMessage, UUID designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_AGGREGATE_UPDATE_COMPLETED);
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        TileAggregateUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileAggregateUpdateCompleted.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
    }

    public static void assertExpectedDesignDocumentUpdateRequestedMessage(InputMessage actualMessage, UUID designId, String data, String checksum, String status) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED);
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        DesignDocumentUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDocumentUpdateRequested.class);
        assertThat(actualEvent.getUserId()).isEqualTo(TestConstants.USER_ID);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getCommandId()).isNotNull();
        assertThat(actualEvent.getRevision()).isNotNull();
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getStatus()).isEqualTo(status);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getLevels()).isEqualTo(3);
        assertThat(actualEvent.getTiles()).isNotNull();
    }

    public static void assertExpectedDesignDocumentDeleteRequestedMessage(InputMessage actualMessage, UUID designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_DELETED_REQUESTED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        DesignDocumentDeleteRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDocumentDeleteRequested.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
    }
}
