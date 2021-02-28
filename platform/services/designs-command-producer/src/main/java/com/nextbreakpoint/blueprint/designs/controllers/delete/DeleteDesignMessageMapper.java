package com.nextbreakpoint.blueprint.designs.controllers.delete;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.command.DeleteDesign;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignMessageMapper implements Mapper<DeleteDesign, Message> {
    private final String messageSource;

    public DeleteDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(DeleteDesign command) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_DELETE, Json.encode(command), messageSource, command.getUuid().toString(), System.currentTimeMillis());
    }
}
