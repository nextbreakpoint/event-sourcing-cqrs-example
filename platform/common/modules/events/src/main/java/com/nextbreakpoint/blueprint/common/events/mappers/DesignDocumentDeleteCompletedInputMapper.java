package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;

public class DesignDocumentDeleteCompletedInputMapper implements Mapper<InputMessage, DesignDocumentDeleteCompleted> {
    private final Logger logger = LoggerFactory.getLogger(DesignDocumentDeleteCompletedInputMapper.class.getName());

    @Override
    public DesignDocumentDeleteCompleted transform(InputMessage message) {
        if (!message.getValue().getType().equals(DesignDocumentDeleteCompleted.TYPE)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), DesignDocumentDeleteCompleted.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
