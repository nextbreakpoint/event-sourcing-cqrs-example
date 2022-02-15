package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequested;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateRequestedOutputMapper implements MessageMapper<TileAggregateUpdateRequested, OutputMessage> {
    private final String messageSource;

    public TileAggregateUpdateRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(Tracing trace, TileAggregateUpdateRequested event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TileAggregateUpdateRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
