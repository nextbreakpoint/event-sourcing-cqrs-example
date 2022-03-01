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
    public OutputMessage transform(Tracing trace, DesignUpdateCommand event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TimeUUID.next().toString(),
                        DesignUpdateCommand.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
