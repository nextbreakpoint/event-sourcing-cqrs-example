package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;

import java.util.Objects;
import java.util.UUID;

public class TileRenderCompletedOutputMapper implements MessageMapper<TileRenderCompleted, OutputMessage> {
    private final String messageSource;

    public TileRenderCompletedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(Tracing trace, TileRenderCompleted event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TileRenderCompleted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
