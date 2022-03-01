package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignUpdateRequestedOutputMapper implements MessageMapper<DesignUpdateRequested, OutputMessage> {
    private final String messageSource;

    public DesignUpdateRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(Tracing trace, DesignUpdateRequested event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TimeUUID.next().toString(),
                        DesignUpdateRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
