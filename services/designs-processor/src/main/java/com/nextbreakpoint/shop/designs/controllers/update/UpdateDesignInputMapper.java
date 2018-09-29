package com.nextbreakpoint.shop.designs.controllers.update;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.commands.UpdateDesignCommand;
import com.nextbreakpoint.shop.designs.model.CommandRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UpdateDesignInputMapper implements Mapper<CommandRequest, UpdateDesignCommand> {
    private final Logger logger = LoggerFactory.getLogger(UpdateDesignInputMapper.class.getName());

    @Override
    public UpdateDesignCommand transform(CommandRequest request) {
        if (!request.getMessage().getMessageType().equals(MessageType.DESIGN_UPDATE)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_UPDATE);
        }
        try {
            return Json.decodeValue(request.getMessage().getMessageBody(), UpdateDesignCommand.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + request.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
