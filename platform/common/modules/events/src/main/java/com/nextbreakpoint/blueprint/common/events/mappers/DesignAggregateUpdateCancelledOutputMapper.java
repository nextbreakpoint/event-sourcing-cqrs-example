package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCancelled;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateCancelledOutputMapper implements MessageMapper<DesignAggregateUpdateCancelled, OutputMessage> {
    private final String messageSource;

    public DesignAggregateUpdateCancelledOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignAggregateUpdateCancelled event, Tracing trace) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignAggregateUpdateCancelled.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
