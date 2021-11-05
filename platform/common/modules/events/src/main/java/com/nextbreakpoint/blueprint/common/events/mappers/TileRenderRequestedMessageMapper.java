package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class TileRenderRequestedMessageMapper implements Mapper<TileRenderRequested, Message> {
    private final String messageSource;

    public TileRenderRequestedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(TileRenderRequested event) {
        return new Message(
                UUID.randomUUID().toString(),
                MessageType.TILE_RENDER_REQUESTED,
                Json.encode(event),
                messageSource,
                event.getUuid().toString(),
                System.currentTimeMillis()
        );
    }
}
