package com.nextbreakpoint.shop.designs.controllers.change;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.designs.model.RecordAndMessage;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DesignChangeInputMapper implements Mapper<RecordAndMessage, DesignChangedEvent> {
    private final Logger logger = LoggerFactory.getLogger(DesignChangeInputMapper.class.getName());

    @Override
    public DesignChangedEvent transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.DESIGN_CHANGED)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_CHANGED);
        }
        try {
            return Json.decodeValue(input.getMessage().getMessageBody(), DesignChangedEvent.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
