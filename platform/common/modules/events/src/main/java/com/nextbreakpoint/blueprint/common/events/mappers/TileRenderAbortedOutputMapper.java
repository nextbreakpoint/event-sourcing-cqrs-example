package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;
import io.vertx.core.json.Json;

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
                        MessageType.TILE_RENDER_ABORTED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
