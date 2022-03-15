package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class TileRenderRequestedOutputMapper implements MessageMapper<TileRenderRequested, OutputMessage> {
    private final String messageSource;
    private final Function<TileRenderRequested, String> keyMapper;

    public TileRenderRequestedOutputMapper(String messageSource, Function<TileRenderRequested, String> keyMapper) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.keyMapper = Objects.requireNonNull(keyMapper);
    }

    public TileRenderRequestedOutputMapper(String messageSource) {
        this(messageSource, event -> event.getDesignId().toString());
    }

    @Override
    public OutputMessage transform(TileRenderRequested event) {
        return new OutputMessage(
                keyMapper.apply(event),
                new Payload(
                        UUID.randomUUID(),
                        TileRenderRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
