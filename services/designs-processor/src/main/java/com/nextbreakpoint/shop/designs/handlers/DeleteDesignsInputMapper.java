package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.DeleteDesignsEvent;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.common.MessageType;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DeleteDesignsInputMapper implements Mapper<Message, DeleteDesignsEvent> {
    @Override
    public DeleteDesignsEvent transform(Message message) {
        if (!message.getMessageType().equals(MessageType.DESIGNS_DELETE)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGNS_DELETE);
        }
        try {
            return Json.decodeValue(message.getMessageBody(), DeleteDesignsEvent.class);
        } catch (DecodeException e) {
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
