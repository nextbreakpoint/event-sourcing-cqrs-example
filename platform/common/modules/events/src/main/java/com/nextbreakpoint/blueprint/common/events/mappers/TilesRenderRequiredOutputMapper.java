package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TilesRenderRequired;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class TilesRenderRequiredOutputMapper implements MessageMapper<TilesRenderRequired, OutputMessage> {
    private final String messageSource;
    private final Function<TilesRenderRequired, String> keyMapper;

    public TilesRenderRequiredOutputMapper(String messageSource, Function<TilesRenderRequired, String> keyMapper) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.keyMapper = Objects.requireNonNull(keyMapper);
    }

    public TilesRenderRequiredOutputMapper(String messageSource) {
        this(messageSource, event -> event.getDesignId().toString());
    }

    @Override
    public OutputMessage transform(TilesRenderRequired event, Tracing trace) {
        return new OutputMessage(
                keyMapper.apply(event),
                new Payload(
                        UUID.randomUUID(),
                        TilesRenderRequired.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
