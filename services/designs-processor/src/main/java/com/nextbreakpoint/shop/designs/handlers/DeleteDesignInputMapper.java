package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.DeleteDesignEvent;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.common.MessageType;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DeleteDesignInputMapper implements Mapper<Message, DeleteDesignEvent> {
    @Override
    public DeleteDesignEvent transform(Message message) {
        if (!message.getMessageType().equals(MessageType.DESIGN_DELETE)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_DELETE);
        }
        try {
            return Json.decodeValue(message.getMessageBody(), DeleteDesignEvent.class);
        } catch (DecodeException e) {
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
