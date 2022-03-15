package com.nextbreakpoint.blueprint.common.commands.mappers;

import com.nextbreakpoint.blueprint.common.commands.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.*;

import java.util.Objects;
import java.util.UUID;

public class DesignUpdateCommandOutputMapper implements MessageMapper<DesignUpdateCommand, OutputMessage> {
    private final String messageSource;

    public DesignUpdateCommandOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignUpdateCommand event, Tracing trace) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignUpdateCommand.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
