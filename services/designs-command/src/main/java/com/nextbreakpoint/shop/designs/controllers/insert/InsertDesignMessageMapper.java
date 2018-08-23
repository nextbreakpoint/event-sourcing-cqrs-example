package com.nextbreakpoint.shop.designs.controllers.insert;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.events.InsertDesignEvent;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignMessageMapper implements Mapper<InsertDesignEvent, Message> {
    private final String messageSource;

    public InsertDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(InsertDesignEvent event) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_INSERT, Json.encode(event), messageSource, event.getUuid().toString(), System.currentTimeMillis());
    }
}
