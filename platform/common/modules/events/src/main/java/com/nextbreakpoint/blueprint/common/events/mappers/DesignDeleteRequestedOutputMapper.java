package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignDeleteRequestedOutputMapper implements MessageMapper<DesignDeleteRequested, OutputMessage> {
    private final String messageSource;

    public DesignDeleteRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(Tracing trace, DesignDeleteRequested event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TimeUUID.next().toString(),
                        DesignDeleteRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
