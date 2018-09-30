package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import com.nextbreakpoint.shop.designs.model.RecordAndMessage;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DeleteDesignInputMapper implements Mapper<RecordAndMessage, DeleteDesignCommand> {
    private final Logger logger = LoggerFactory.getLogger(DeleteDesignInputMapper.class.getName());

    @Override
    public DeleteDesignCommand transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.DESIGN_DELETE)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_DELETE);
        }
        try {
            return Json.decodeValue(input.getMessage().getMessageBody(), DeleteDesignCommand.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
