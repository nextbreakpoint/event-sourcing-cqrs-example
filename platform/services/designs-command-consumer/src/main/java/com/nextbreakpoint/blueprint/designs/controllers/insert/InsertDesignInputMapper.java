package com.nextbreakpoint.blueprint.designs.controllers.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.command.InsertDesign;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class InsertDesignInputMapper implements Mapper<RecordAndMessage, InsertDesign> {
    private final Logger logger = LoggerFactory.getLogger(InsertDesignInputMapper.class.getName());

    @Override
    public InsertDesign transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.DESIGN_INSERT)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_INSERT);
        }
        try {
            return Json.decodeValue(input.getMessage().getMessageBody(), InsertDesign.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
