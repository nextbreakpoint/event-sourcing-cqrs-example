package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;

import java.util.Objects;
import java.util.UUID;

public class TileRenderAbortedOutputMapper implements MessageMapper<TileRenderAborted, OutputMessage> {
    private final String messageSource;

    public TileRenderAbortedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(Tracing trace, TileRenderAborted event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TileRenderAborted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
