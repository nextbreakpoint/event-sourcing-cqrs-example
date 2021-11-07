package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateCompletedMessageMapper implements Mapper<TileAggregateUpdateCompleted, Message> {
    private final String messageSource;

    public TileAggregateUpdateCompletedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(TileAggregateUpdateCompleted event) {
        return new Message(
                UUID.randomUUID().toString(),
                MessageType.TILE_AGGREGATE_UPDATE_COMPLETED,
                Json.encode(event),
                messageSource,
                event.getUuid().toString(),
                System.currentTimeMillis()
        );
    }
}
