package com.nextbreakpoint.blueprint.common.commands.mappers;

import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;

import java.util.Objects;
import java.util.UUID;

public class DesignInsertCommandOutputMapper implements Mapper<DesignInsertCommand, OutputMessage> {
    private final String messageSource;

    public DesignInsertCommandOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignInsertCommand event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignInsertCommand.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
