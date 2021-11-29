package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import io.vertx.core.json.Json;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAssertions {
    private TestAssertions() {}

    public static void assertExpectedMessage(Row row, OutputMessage message) {
        final String actualType = row.getString("MESSAGE_TYPE");
        final String actualValue = row.getString("MESSAGE_VALUE");
        final UUID actualUuid = row.getUuid("MESSAGE_UUID");
        final String actualSource = row.getString("MESSAGE_SOURCE");
        final String actualKey = row.getString("MESSAGE_KEY");
        final Instant actualTimestamp = row.getInstant("MESSAGE_TIMESTAMP");
        final Long actualOffset = row.getLong("MESSAGE_OFFSET");
        assertThat(actualOffset).isNotNull();
        assertThat(actualUuid).isEqualTo(message.getValue().getUuid());
        assertThat(actualValue).isEqualTo(message.getValue().getData());
        assertThat(actualType).isEqualTo(message.getValue().getType());
        assertThat(actualSource).isEqualTo(message.getValue().getSource());
        assertThat(actualKey).isEqualTo(message.getKey());
        assertThat(actualTimestamp).isNotNull();
    }

    public static void assertExpectedDesign(Row row, String data, String status, List<Tiles> tiles) {
        final String actualJson = row.getString("DESIGN_DATA");
        final String actualStatus = row.getString("DESIGN_STATUS");
        final String actualChecksum = row.getString("DESIGN_CHECKSUM");
        final int actualLevels = row.getInt("DESIGN_LEVELS");
        final Map<Integer, UdtValue> tilesMap = row.getMap("DESIGN_TILES", Integer.class, UdtValue.class);
        final List<Tiles> actualTiles = tilesMap.entrySet().stream()
                .map(entry -> convertToTiles(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        assertThat(actualJson).isEqualTo(data);
        assertThat(actualStatus).isEqualTo(status);
        assertThat(actualChecksum).isNotNull();
        assertThat(actualLevels).isEqualTo(TestConstants.LEVELS);
        assertThat(actualTiles).isEqualTo(tiles);
    }

    public static void assertExpectedDesignAggregateUpdateRequestedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_AGGREGATE_UPDATE_REQUESTED);
        DesignAggregateUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignAggregateUpdateRequested.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getEvid()).isNotNull();
    }

    public static void assertExpectedDesignAggregateUpdateCompletedMessage(UUID designId, InputMessage actualMessage, String data, String checksum, String status) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_AGGREGATE_UPDATE_COMPLETED);
        DesignAggregateUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignAggregateUpdateCompleted.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getEvid()).isNotNull();
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getStatus()).isEqualTo(status);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
    }

    public static void assertExpectedTileRenderRequestedMessage(InputMessage actualMessage, String partitionKey) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(partitionKey);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_RENDER_REQUESTED);
        assertThat(actualMessage.getValue()).isNotNull();
    }

    public static void assertExpectedTileRenderRequestedEvent(UUID designId, TileRenderRequested actualEvent, String data, String checksum) {
        assertThat(actualEvent.getEvid()).isNotNull();
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getLevel()).isNotNull();
        assertThat(actualEvent.getRow()).isNotNull();
        assertThat(actualEvent.getCol()).isNotNull();
    }

    public static void assertExpectedTileRenderCompletedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_RENDER_COMPLETED);
        assertThat(actualMessage.getValue()).isNotNull();
    }

    public static void assertExpectedTileRenderCompletedEvent(UUID designId, TileRenderCompleted actualEvent, String checksum, String status) {
        assertThat(actualEvent.getEvid()).isNotNull();
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getStatus()).isEqualTo(status);
        assertThat(actualEvent.getLevel()).isNotNull();
        assertThat(actualEvent.getRow()).isNotNull();
        assertThat(actualEvent.getCol()).isNotNull();
    }

    public static void assertExpectedTileAggregateUpdateRequiredMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_AGGREGATE_UPDATE_REQUIRED);
        TileAggregateUpdateRequired actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileAggregateUpdateRequired.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getEvid()).isNotNull();
    }

    public static void assertExpectedTileAggregateUpdateRequestedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_AGGREGATE_UPDATE_REQUESTED);
        TileAggregateUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileAggregateUpdateRequested.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getEvid()).isNotNull();
    }

    public static void assertExpectedTileAggregateUpdateCompletedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_AGGREGATE_UPDATE_COMPLETED);
        TileAggregateUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileAggregateUpdateCompleted.class);
        assertThat(actualEvent.getUuid()).isEqualTo(designId);
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getEvid()).isNotNull();
    }

    @NotNull
    private static Tiles convertToTiles(Integer level, UdtValue udtValue) {
        return new Tiles(level, udtValue.getInt("REQUESTED"), udtValue.getSet("COMPLETED", Integer.class), udtValue.getSet("FAILED", Integer.class));
    }
}
