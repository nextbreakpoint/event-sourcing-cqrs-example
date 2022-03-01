package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAbortRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignAbortRequestedOutputMapper implements MessageMapper<DesignAbortRequested, OutputMessage> {
    private final String messageSource;

    public DesignAbortRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(Tracing trace, DesignAbortRequested event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TimeUUID.next().toString(),
                        DesignAbortRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
