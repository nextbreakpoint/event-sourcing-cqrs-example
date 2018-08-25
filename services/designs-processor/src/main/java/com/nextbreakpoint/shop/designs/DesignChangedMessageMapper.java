package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignChangedMessageMapper implements Mapper<DesignChangedEvent, Message> {
    private final String messageSource;

    public DesignChangedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(DesignChangedEvent event) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_CHANGED, Json.encode(event), messageSource, event.getUuid().toString(), System.currentTimeMillis());
    }
}
