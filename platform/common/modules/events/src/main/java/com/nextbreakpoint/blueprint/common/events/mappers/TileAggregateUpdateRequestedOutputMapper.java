package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateRequestedOutputMapper implements Mapper<TileAggregateUpdateRequested, OutputMessage> {
    private final String messageSource;

    public TileAggregateUpdateRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(TileAggregateUpdateRequested event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        MessageType.TILE_AGGREGATE_UPDATE_REQUESTED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
