package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class UpdateDesignMessageMapper implements Mapper<UpdateDesignCommand, OutputMessage> {
    private final String messageSource;

    public UpdateDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(UpdateDesignCommand command) {
        return new OutputMessage(command.getUuid().toString(), new Payload(UUID.randomUUID(), MessageType.DESIGN_UPDATE_REQUESTED, Json.encode(command), messageSource));
    }
}
