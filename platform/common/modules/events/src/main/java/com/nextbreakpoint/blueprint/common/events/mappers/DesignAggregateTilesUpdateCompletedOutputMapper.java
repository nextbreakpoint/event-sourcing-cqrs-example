package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateTilesUpdateCompleted;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateTilesUpdateCompletedOutputMapper implements MessageMapper<DesignAggregateTilesUpdateCompleted, OutputMessage> {
    private final String messageSource;

    public DesignAggregateTilesUpdateCompletedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignAggregateTilesUpdateCompleted event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignAggregateTilesUpdateCompleted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
