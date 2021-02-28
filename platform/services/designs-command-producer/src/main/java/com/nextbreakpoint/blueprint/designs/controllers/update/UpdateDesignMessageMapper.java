package com.nextbreakpoint.blueprint.designs.controllers.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class UpdateDesignMessageMapper implements Mapper<UpdateDesign, Message> {
    private final String messageSource;

    public UpdateDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(UpdateDesign command) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_UPDATE, Json.encode(command), messageSource, command.getUuid().toString(), System.currentTimeMillis());
    }
}
