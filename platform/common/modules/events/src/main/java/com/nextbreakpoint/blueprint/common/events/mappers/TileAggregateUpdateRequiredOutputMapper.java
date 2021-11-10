package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateRequiredOutputMapper implements Mapper<TileAggregateUpdateRequired, OutputMessage> {
    private final String messageSource;

    public TileAggregateUpdateRequiredOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(TileAggregateUpdateRequired event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        MessageType.TILE_AGGREGATE_UPDATE_REQUIRED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
