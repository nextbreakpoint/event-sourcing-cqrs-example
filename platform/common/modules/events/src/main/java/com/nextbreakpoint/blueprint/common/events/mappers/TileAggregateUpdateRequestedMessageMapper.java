package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateRequestedMessageMapper implements Mapper<TileAggregateUpdateRequested, Message> {
    private final String messageSource;

    public TileAggregateUpdateRequestedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(TileAggregateUpdateRequested event) {
        return new Message(
                UUID.randomUUID().toString(),
                MessageType.TILE_AGGREGATE_UPDATE_REQUESTED,
                Json.encode(event),
                messageSource,
                event.getUuid().toString(),
                System.currentTimeMillis()
        );
    }
}
