package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import io.vertx.core.json.Json;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAssertions {
    private TestAssertions() {}

    public static void assertExpectedTileRenderCompletedMessage(TileRenderRequested tileRenderRequested, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(tileRenderRequested.getUuid().toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.TILE_RENDER_COMPLETED);
        assertThat(actualMessage.getValue()).isNotNull();
        TileRenderCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileRenderCompleted.class);
        assertThat(actualEvent.getEvid()).isNotNull();
        assertThat(actualEvent.getEsid()).isNotNull();
        assertThat(actualEvent.getUuid()).isEqualTo(tileRenderRequested.getUuid());
        assertThat(actualEvent.getLevel()).isEqualTo(tileRenderRequested.getLevel());
        assertThat(actualEvent.getRow()).isEqualTo(tileRenderRequested.getRow());
        assertThat(actualEvent.getCol()).isEqualTo(tileRenderRequested.getCol());
    }
}
