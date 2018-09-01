package com.nextbreakpoint.shop.common.vertx;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignChangedMapper implements Mapper<DesignChangedEvent, Message> {
    private final String messageSource;

    public DesignChangedMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(DesignChangedEvent event) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_CHANGED, Json.encode(event), messageSource, event.getUuid().toString(), System.currentTimeMillis());
    }
}
