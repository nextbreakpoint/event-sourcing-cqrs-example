package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndEvent;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndMessage;
import com.nextbreakpoint.blueprint.designs.events.DesignInsertRequested;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DesignInsertRequestedInputMapper implements Mapper<RecordAndMessage, RecordAndEvent<DesignInsertRequested>> {
    private final Logger logger = LoggerFactory.getLogger(DesignInsertRequestedInputMapper.class.getName());

    @Override
    public RecordAndEvent<DesignInsertRequested> transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.DESIGN_INSERT_REQUESTED)) {
            throw new IllegalArgumentException("Unexpected message type: " + input.getMessage().getMessageType());
        }
        try {
            return new RecordAndEvent<>(input.getRecord(), Json.decodeValue(input.getMessage().getMessageBody(), DesignInsertRequested.class));
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}