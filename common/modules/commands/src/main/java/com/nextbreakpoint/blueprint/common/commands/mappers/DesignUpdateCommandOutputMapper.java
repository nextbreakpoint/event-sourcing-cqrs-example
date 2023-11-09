package com.nextbreakpoint.blueprint.common.commands.mappers;

import com.nextbreakpoint.blueprint.common.commands.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;

import java.util.Objects;
import java.util.UUID;

public class DesignUpdateCommandOutputMapper implements Mapper<DesignUpdateCommand, OutputMessage> {
    private final String messageSource;

    public DesignUpdateCommandOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignUpdateCommand event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignUpdateCommand.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
