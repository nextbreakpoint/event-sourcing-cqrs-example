package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.common.MessageType;
import com.nextbreakpoint.shop.designs.model.InsertDesignEvent;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignMessageMapper implements Mapper<InsertDesignEvent, Message> {
    private final String messageSource;

    public InsertDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(InsertDesignEvent request) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_INSERT, Json.encode(request), messageSource, request.getUuid().toString(), System.currentTimeMillis());
    }
}
