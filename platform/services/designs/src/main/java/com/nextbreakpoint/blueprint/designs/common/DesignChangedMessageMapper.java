package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignChangedMessageMapper implements Mapper<DesignChanged, Message> {
    private final String messageSource;

    public DesignChangedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(DesignChanged event) {
        return new Message(
                UUID.randomUUID().toString(),
                MessageType.DESIGN_CHANGED,
                Json.encode(event),
                messageSource,
                event.getUuid().toString(),
                System.currentTimeMillis()
        );
    }
}
