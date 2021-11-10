package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class TileRenderCompletedMessageMapper implements Mapper<TileRenderCompleted, Message> {
    private final String messageSource;

    public TileRenderCompletedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(TileRenderCompleted event) {
        return new Message(
                UUID.randomUUID(),
                MessageType.TILE_RENDER_COMPLETED,
                Json.encode(event),
                messageSource,
                event.getUuid().toString(),
                System.currentTimeMillis()
        );
    }
}
