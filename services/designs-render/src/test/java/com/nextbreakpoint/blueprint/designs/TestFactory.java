package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.MessageUtils;
import com.nextbreakpoint.blueprint.designs.common.Render;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_REQUESTED;

public class TestFactory {
    private TestFactory() {}

    @NotNull
    public static InputMessage<TileRenderRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, TileRenderRequested tileRenderRequested) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, Render.createRenderKey(tileRenderRequested), TILE_RENDER_REQUESTED, messageId, tileRenderRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<TileRenderCompleted> createOutputMessage(UUID messageId, TileRenderCompleted tileRenderCompleted) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, Render.createRenderKey(tileRenderCompleted), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted
        );
    }
}
