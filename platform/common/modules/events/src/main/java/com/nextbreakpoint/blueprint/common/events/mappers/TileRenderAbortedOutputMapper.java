package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class TileRenderAbortedOutputMapper implements MessageMapper<TileRenderAborted, OutputMessage> {
    private final String messageSource;
    private final Function<TileRenderAborted, String> keyMapper;

    public TileRenderAbortedOutputMapper(String messageSource, Function<TileRenderAborted, String> keyMapper) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.keyMapper = Objects.requireNonNull(keyMapper);
    }

    public TileRenderAbortedOutputMapper(String messageSource) {
        this(messageSource, event -> event.getDesignId().toString());
    }

    @Override
    public OutputMessage transform(TileRenderAborted event) {
        return new OutputMessage(
                keyMapper.apply(event),
                new Payload(
                        UUID.randomUUID(),
                        TileRenderAborted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
