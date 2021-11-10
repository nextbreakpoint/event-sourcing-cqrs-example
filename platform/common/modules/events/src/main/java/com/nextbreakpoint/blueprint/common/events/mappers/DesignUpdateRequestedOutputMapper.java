package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignUpdateRequestedOutputMapper implements Mapper<DesignUpdateRequested, OutputMessage> {
    private final String messageSource;

    public DesignUpdateRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignUpdateRequested event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        MessageType.DESIGN_UPDATE_REQUESTED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
