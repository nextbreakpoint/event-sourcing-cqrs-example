package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class TileRenderCompletedOutputMapper implements Mapper<TileRenderCompleted, OutputMessage> {
    private final String messageSource;

    public TileRenderCompletedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(TileRenderCompleted event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TileRenderCompleted.TYPE,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
