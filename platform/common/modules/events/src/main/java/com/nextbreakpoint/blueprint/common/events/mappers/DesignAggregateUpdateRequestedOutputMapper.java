package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateRequestedOutputMapper implements Mapper<DesignAggregateUpdateRequested, OutputMessage> {
    private final String messageSource;

    public DesignAggregateUpdateRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignAggregateUpdateRequested event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        MessageType.DESIGN_AGGREGATE_UPDATE_REQUESTED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
