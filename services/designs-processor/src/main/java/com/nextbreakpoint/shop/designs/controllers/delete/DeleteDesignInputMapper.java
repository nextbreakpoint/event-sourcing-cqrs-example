package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DeleteDesignInputMapper implements Mapper<Message, DeleteDesignCommand> {
    private final Logger logger = LoggerFactory.getLogger(DeleteDesignInputMapper.class.getName());

    @Override
    public DeleteDesignCommand transform(Message message) {
        if (!message.getMessageType().equals(MessageType.DESIGN_DELETE)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_DELETE);
        }
        try {
            return Json.decodeValue(message.getMessageBody(), DeleteDesignCommand.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
