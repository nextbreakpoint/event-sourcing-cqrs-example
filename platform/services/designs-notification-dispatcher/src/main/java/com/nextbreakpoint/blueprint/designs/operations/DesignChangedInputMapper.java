package com.nextbreakpoint.blueprint.designs.operations;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DesignChangedInputMapper implements Mapper<Message, DesignChanged> {
    @Override
    public DesignChanged transform(Message message) {
        if (!message.getMessageType().equals(MessageType.DESIGN_CHANGED)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getMessageType());
        }
        try {
            return Json.decodeValue(message.getMessageBody(), DesignChanged.class);
        } catch (DecodeException e) {
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
