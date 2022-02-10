package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;

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
