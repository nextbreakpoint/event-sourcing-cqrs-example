package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateCompletedOutputMapper implements Mapper<DesignAggregateUpdateCompleted, OutputMessage> {
    private final String messageSource;

    public DesignAggregateUpdateCompletedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignAggregateUpdateCompleted event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        MessageType.DESIGN_AGGREGATE_UPDATE_COMPLETED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
