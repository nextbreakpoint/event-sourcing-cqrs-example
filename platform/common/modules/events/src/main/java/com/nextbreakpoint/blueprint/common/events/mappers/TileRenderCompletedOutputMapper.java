package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class TileRenderCompletedOutputMapper implements MessageMapper<TileRenderCompleted, OutputMessage> {
    private final String messageSource;
    private final Function<TileRenderCompleted, String> keyMapper;

    public TileRenderCompletedOutputMapper(String messageSource, Function<TileRenderCompleted, String> keyMapper) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.keyMapper = Objects.requireNonNull(keyMapper);
    }

    public TileRenderCompletedOutputMapper(String messageSource) {
        this(messageSource, event -> event.getDesignId().toString());
    }

    @Override
    public OutputMessage transform(TileRenderCompleted event) {
        return new OutputMessage(
                keyMapper.apply(event),
                new Payload(
                        UUID.randomUUID(),
                        TileRenderCompleted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
