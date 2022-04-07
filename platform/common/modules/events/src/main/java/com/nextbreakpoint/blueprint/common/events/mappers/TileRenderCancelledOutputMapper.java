package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.TileRenderCancelled;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class TileRenderCancelledOutputMapper implements MessageMapper<TileRenderCancelled, OutputMessage> {
    private final String messageSource;
    private final Function<TileRenderCancelled, String> keyMapper;

    public TileRenderCancelledOutputMapper(String messageSource, Function<TileRenderCancelled, String> keyMapper) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.keyMapper = Objects.requireNonNull(keyMapper);
    }

    public TileRenderCancelledOutputMapper(String messageSource) {
        this(messageSource, event -> event.getDesignId().toString());
    }

    @Override
    public OutputMessage transform(TileRenderCancelled event) {
        return new OutputMessage(
                keyMapper.apply(event),
                new Payload(
                        UUID.randomUUID(),
                        TileRenderCancelled.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
