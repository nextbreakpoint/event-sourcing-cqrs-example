package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateCompletedOutputMapper implements MessageMapper<DesignAggregateUpdateCompleted, OutputMessage> {
    private final String messageSource;

    public DesignAggregateUpdateCompletedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignAggregateUpdateCompleted event, Tracing trace) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignAggregateUpdateCompleted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
