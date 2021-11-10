package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateRequiredMessageMapper implements Mapper<TileAggregateUpdateRequired, Message> {
    private final String messageSource;

    public TileAggregateUpdateRequiredMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(TileAggregateUpdateRequired event) {
        return new Message(
                UUID.randomUUID(),
                MessageType.TILE_AGGREGATE_UPDATE_REQUIRED,
                Json.encode(event),
                messageSource,
                event.getUuid().toString(),
                System.currentTimeMillis()
        );
    }
}
