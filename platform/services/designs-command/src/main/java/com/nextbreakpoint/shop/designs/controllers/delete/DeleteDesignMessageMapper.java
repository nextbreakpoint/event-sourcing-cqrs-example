package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignMessageMapper implements Mapper<DeleteDesignCommand, Message> {
    private final String messageSource;

    public DeleteDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(DeleteDesignCommand command) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_DELETE, Json.encode(command), messageSource, command.getUuid().toString(), System.currentTimeMillis());
    }
}
