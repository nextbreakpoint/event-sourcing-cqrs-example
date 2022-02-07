package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;

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
                        DesignAggregateUpdateRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
