package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateCompletedOutputMapper implements Mapper<TileAggregateUpdateCompleted, OutputMessage> {
    private final String messageSource;

    public TileAggregateUpdateCompletedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(TileAggregateUpdateCompleted event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        MessageType.TILE_AGGREGATE_UPDATE_COMPLETED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
