package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.common.MessageType;
import com.nextbreakpoint.shop.common.DeleteDesignEvent;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignMessageMapper implements Mapper<DeleteDesignEvent, Message> {
    private final String messageSource;

    public DeleteDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(DeleteDesignEvent event) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_DELETE, Json.encode(event), messageSource, event.getUuid().toString(), System.currentTimeMillis());
    }
}
