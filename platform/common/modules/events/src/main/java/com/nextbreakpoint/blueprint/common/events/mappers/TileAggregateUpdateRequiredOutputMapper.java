package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateRequiredOutputMapper implements MessageMapper<TileAggregateUpdateRequired, OutputMessage> {
    private final String messageSource;

    public TileAggregateUpdateRequiredOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(Tracing trace, TileAggregateUpdateRequired event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TileAggregateUpdateRequired.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
