package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DesignDeleteRequestedInputMapper implements Mapper<InputMessage, DesignDeleteRequested> {
    private final Logger logger = LoggerFactory.getLogger(DesignDeleteRequestedInputMapper.class.getName());

    @Override
    public DesignDeleteRequested transform(InputMessage message) {
        if (!message.getValue().getType().equals(DesignDeleteRequested.TYPE)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), DesignDeleteRequested.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
