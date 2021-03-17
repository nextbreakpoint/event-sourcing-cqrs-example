package com.nextbreakpoint.blueprint.designs.controllers.change;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DesignChangeInputMapper implements Mapper<RecordAndMessage, DesignChanged> {
    private final Logger logger = LoggerFactory.getLogger(DesignChangeInputMapper.class.getName());

    @Override
    public DesignChanged transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.DESIGN_CHANGED)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_CHANGED);
        }
        try {
            return Json.decodeValue(input.getMessage().getMessageBody(), DesignChanged.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
