package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.Payload;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignMessageMapper implements Mapper<InsertDesignCommand, Message> {
    private final String messageSource;

    public InsertDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(InsertDesignCommand command) {
        return new Message(command.getUuid().toString(), 0,  System.currentTimeMillis(), new Payload(UUID.randomUUID(), MessageType.DESIGN_INSERT_REQUESTED, Json.encode(command), messageSource));
    }
}
