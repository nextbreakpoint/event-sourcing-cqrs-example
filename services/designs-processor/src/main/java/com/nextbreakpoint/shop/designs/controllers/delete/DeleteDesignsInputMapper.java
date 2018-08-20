package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.events.DeleteDesignsEvent;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageType;
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
