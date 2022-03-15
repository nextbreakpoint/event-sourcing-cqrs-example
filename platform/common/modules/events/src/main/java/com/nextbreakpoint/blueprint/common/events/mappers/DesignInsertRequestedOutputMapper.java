package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignInsertRequestedOutputMapper implements MessageMapper<DesignInsertRequested, OutputMessage> {
    private final String messageSource;

    public DesignInsertRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignInsertRequested event, Tracing trace) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignInsertRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
