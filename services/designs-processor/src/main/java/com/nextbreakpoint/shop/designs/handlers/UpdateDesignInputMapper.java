package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.common.MessageType;
import com.nextbreakpoint.shop.common.UpdateDesignEvent;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class UpdateDesignInputMapper implements Mapper<Message, UpdateDesignEvent> {
    @Override
    public UpdateDesignEvent transform(Message message) {
        if (!message.getMessageType().equals(MessageType.DESIGN_UPDATE)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_UPDATE);
        }
        try {
            return Json.decodeValue(message.getMessageBody(), UpdateDesignEvent.class);
        } catch (DecodeException e) {
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
