package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateTilesUpdateRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateTilesUpdateRequestedOutputMapper implements MessageMapper<DesignAggregateTilesUpdateRequested, OutputMessage> {
    private final String messageSource;

    public DesignAggregateTilesUpdateRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignAggregateTilesUpdateRequested event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignAggregateTilesUpdateRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
