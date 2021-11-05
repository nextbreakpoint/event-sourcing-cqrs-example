package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Message;

import java.util.Objects;
import java.util.UUID;

public class MessageAndMetadata {
    private final Message message;
    private final UUID timeUuid;

    public MessageAndMetadata(Message message, UUID timeUuid) {
        this.message = Objects.requireNonNull(message);
        this.timeUuid = Objects.requireNonNull(timeUuid);;
    }

    public Message message() {
        return message;
    }

    public UUID timeUUID() {
        return timeUuid;
    }
}
