package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;

import java.util.Objects;
import java.util.UUID;

public class TileRenderAbortedOutputMapper implements Mapper<TileRenderAborted, OutputMessage> {
    private final String messageSource;

    public TileRenderAbortedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(TileRenderAborted event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TileRenderAborted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
