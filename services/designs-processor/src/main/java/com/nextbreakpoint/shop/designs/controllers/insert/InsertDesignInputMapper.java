package com.nextbreakpoint.shop.designs.controllers.insert;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.designs.model.CommandRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class InsertDesignInputMapper implements Mapper<CommandRequest, InsertDesignCommand> {
    private final Logger logger = LoggerFactory.getLogger(InsertDesignInputMapper.class.getName());

    @Override
    public InsertDesignCommand transform(CommandRequest request) {
        if (!request.getMessage().getMessageType().equals(MessageType.DESIGN_INSERT)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_INSERT);
        }
        try {
            return Json.decodeValue(request.getMessage().getMessageBody(), InsertDesignCommand.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + request.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
