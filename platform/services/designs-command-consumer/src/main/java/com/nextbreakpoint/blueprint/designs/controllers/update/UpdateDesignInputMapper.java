package com.nextbreakpoint.blueprint.designs.controllers.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UpdateDesignInputMapper implements Mapper<RecordAndMessage, UpdateDesign> {
    private final Logger logger = LoggerFactory.getLogger(UpdateDesignInputMapper.class.getName());

    @Override
    public UpdateDesign transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.DESIGN_UPDATE)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_UPDATE);
        }
        try {
            return Json.decodeValue(input.getMessage().getMessageBody(), UpdateDesign.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
