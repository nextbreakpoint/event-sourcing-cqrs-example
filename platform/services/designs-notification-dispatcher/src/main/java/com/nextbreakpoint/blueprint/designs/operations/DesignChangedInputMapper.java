package com.nextbreakpoint.blueprint.designs.operations;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DesignChangedInputMapper implements Mapper<InputMessage, DesignChanged> {
    @Override
    public DesignChanged transform(InputMessage message) {
        if (!message.getValue().getType().equals(MessageType.DESIGN_CHANGED)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), DesignChanged.class);
        } catch (DecodeException e) {
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
