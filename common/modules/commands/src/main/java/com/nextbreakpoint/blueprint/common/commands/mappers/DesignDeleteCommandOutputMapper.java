package com.nextbreakpoint.blueprint.common.commands.mappers;

import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;

import java.util.Objects;
import java.util.UUID;

public class DesignDeleteCommandOutputMapper implements Mapper<DesignDeleteCommand, OutputMessage> {
    private final String messageSource;

    public DesignDeleteCommandOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignDeleteCommand event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignDeleteCommand.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
