package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignDeleteRequestedOutputMapper implements Mapper<DesignDeleteRequested, OutputMessage> {
    private final String messageSource;

    public DesignDeleteRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignDeleteRequested event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        MessageType.DESIGN_DELETE_REQUESTED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
