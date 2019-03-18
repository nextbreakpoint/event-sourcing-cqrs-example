package com.nextbreakpoint.shop.designs.controllers.update;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.commands.UpdateDesignCommand;
import com.nextbreakpoint.shop.designs.model.RecordAndMessage;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UpdateDesignInputMapper implements Mapper<RecordAndMessage, UpdateDesignCommand> {
    private final Logger logger = LoggerFactory.getLogger(UpdateDesignInputMapper.class.getName());

    @Override
    public UpdateDesignCommand transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.DESIGN_UPDATE)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_UPDATE);
        }
        try {
            return Json.decodeValue(input.getMessage().getMessageBody(), UpdateDesignCommand.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
