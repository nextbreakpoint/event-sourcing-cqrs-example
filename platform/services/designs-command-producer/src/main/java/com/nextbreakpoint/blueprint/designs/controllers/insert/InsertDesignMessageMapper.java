package com.nextbreakpoint.blueprint.designs.controllers.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.command.InsertDesign;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignMessageMapper implements Mapper<InsertDesign, Message> {
    private final String messageSource;

    public InsertDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(InsertDesign command) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_INSERT, Json.encode(command), messageSource, command.getUuid().toString(), System.currentTimeMillis());
    }
}
