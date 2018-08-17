package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.InsertDesignEvent;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.common.MessageType;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class InsertDesignInputMapper implements Mapper<Message, InsertDesignEvent> {
    @Override
    public InsertDesignEvent transform(Message message) {
        if (!message.getMessageType().equals(MessageType.DESIGN_INSERT)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_INSERT);
        }
        try {
            return Json.decodeValue(message.getMessageBody(), InsertDesignEvent.class);
        } catch (DecodeException e) {
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
