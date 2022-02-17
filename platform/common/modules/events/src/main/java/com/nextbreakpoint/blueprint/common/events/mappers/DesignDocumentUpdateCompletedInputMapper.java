package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;

public class DesignDocumentUpdateCompletedInputMapper implements Mapper<InputMessage, DesignDocumentUpdateCompleted> {
    private final Logger logger = LoggerFactory.getLogger(DesignDocumentUpdateCompletedInputMapper.class.getName());

    @Override
    public DesignDocumentUpdateCompleted transform(InputMessage message) {
        if (!message.getValue().getType().equals(DesignDocumentUpdateCompleted.TYPE)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), DesignDocumentUpdateCompleted.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}