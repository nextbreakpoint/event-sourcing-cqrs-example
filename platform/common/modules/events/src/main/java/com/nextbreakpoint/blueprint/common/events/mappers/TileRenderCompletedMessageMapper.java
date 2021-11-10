package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.Payload;
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
                event.getUuid().toString(),
                0,
                System.currentTimeMillis(),
                new Payload(
                        UUID.randomUUID(),
                        MessageType.TILE_RENDER_COMPLETED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
