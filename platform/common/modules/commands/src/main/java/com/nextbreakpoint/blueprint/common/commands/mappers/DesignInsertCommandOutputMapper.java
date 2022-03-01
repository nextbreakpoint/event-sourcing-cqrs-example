package com.nextbreakpoint.blueprint.common.commands.mappers;

import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.*;

import java.util.Objects;
import java.util.UUID;

public class DesignInsertCommandOutputMapper implements MessageMapper<DesignInsertCommand, OutputMessage> {
    private final String messageSource;

    public DesignInsertCommandOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(Tracing trace, DesignInsertCommand event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TimeUUID.next().toString(),
                        DesignInsertCommand.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
