package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TilesRendered;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class TilesRenderedOutputMapper implements MessageMapper<TilesRendered, OutputMessage> {
    private final String messageSource;
    private final Function<TilesRendered, String> keyMapper;

    public TilesRenderedOutputMapper(String messageSource, Function<TilesRendered, String> keyMapper) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.keyMapper = Objects.requireNonNull(keyMapper);
    }

    public TilesRenderedOutputMapper(String messageSource) {
        this(messageSource, event -> event.getDesignId().toString());
    }

    @Override
    public OutputMessage transform(TilesRendered event) {
        return new OutputMessage(
                keyMapper.apply(event),
                new Payload(
                        UUID.randomUUID(),
                        TilesRendered.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
