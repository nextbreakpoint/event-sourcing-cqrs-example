package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdated;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdatedOutputMapper implements Mapper<DesignAggregateUpdated, OutputMessage> {
    private final String messageSource;

    public DesignAggregateUpdatedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignAggregateUpdated event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignAggregateUpdated.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
