package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class UpdateDesignMessageMapper implements Mapper<UpdateDesignCommand, Message> {
    private final String messageSource;

    public UpdateDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(UpdateDesignCommand command) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_UPDATE_REQUESTED, Json.encode(command), messageSource, command.getUuid().toString(), System.currentTimeMillis());
    }
}
