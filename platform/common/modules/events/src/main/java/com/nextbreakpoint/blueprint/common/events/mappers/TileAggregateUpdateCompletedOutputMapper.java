package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateCompletedOutputMapper implements MessageMapper<TileAggregateUpdateCompleted, OutputMessage> {
    private final String messageSource;

    public TileAggregateUpdateCompletedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(TileAggregateUpdateCompleted event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TileAggregateUpdateCompleted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
