package com.nextbreakpoint.shop.designs.controllers;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DesignChangedInputMapper implements Mapper<Message, DesignChangedEvent> {
    @Override
    public DesignChangedEvent transform(Message message) {
        if (!message.getMessageType().equals(MessageType.DESIGN_CHANGED)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_CHANGED);
        }
        try {
            return Json.decodeValue(message.getMessageBody(), DesignChangedEvent.class);
        } catch (DecodeException e) {
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
