package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateRequestedOutputMapper implements MessageMapper<DesignAggregateUpdateRequested, OutputMessage> {
    private final String messageSource;

    public DesignAggregateUpdateRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignAggregateUpdateRequested event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignAggregateUpdateRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
