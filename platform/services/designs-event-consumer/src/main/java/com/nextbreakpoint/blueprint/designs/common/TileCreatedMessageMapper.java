package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.TileCreated;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class TileCreatedMessageMapper implements Mapper<TileCreated, Message> {
    private final String messageSource;

    public TileCreatedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(TileCreated event) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_TILE_CREATED, Json.encode(event), messageSource, event.getUuid().toString(), System.currentTimeMillis());
    }
}
