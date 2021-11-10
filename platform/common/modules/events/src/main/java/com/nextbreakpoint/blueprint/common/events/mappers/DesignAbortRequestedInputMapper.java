package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.DesignAbortRequested;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DesignAbortRequestedInputMapper implements Mapper<Message, DesignAbortRequested> {
    private final Logger logger = LoggerFactory.getLogger(DesignAbortRequestedInputMapper.class.getName());

    @Override
    public DesignAbortRequested transform(Message message) {
        if (!message.getPayload().getType().equals(MessageType.DESIGN_ABORT_REQUESTED)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getPayload().getType());
        }
        try {
            return Json.decodeValue(message.getPayload().getData(), DesignAbortRequested.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getPayload(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
