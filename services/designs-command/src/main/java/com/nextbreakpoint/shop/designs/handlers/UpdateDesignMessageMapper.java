package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.common.MessageType;
import com.nextbreakpoint.shop.common.UpdateDesignEvent;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class UpdateDesignMessageMapper implements Mapper<UpdateDesignEvent, Message> {
    private final String messageSource;

    public UpdateDesignMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(UpdateDesignEvent request) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_UPDATE, Json.encode(request), messageSource, request.getUuid().toString(), System.currentTimeMillis());
    }
}
