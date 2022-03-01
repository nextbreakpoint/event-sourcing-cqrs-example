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
    public OutputMessage transform(Tracing trace, DesignAggregateUpdateCompleted event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TimeUUID.next().toString(),
                        DesignAggregateUpdateCompleted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
