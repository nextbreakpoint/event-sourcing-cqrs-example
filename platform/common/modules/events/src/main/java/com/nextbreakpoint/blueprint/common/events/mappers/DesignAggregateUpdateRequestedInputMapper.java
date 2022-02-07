package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.DecodeException;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

public class DesignAggregateUpdateRequestedInputMapper implements Mapper<InputMessage, DesignAggregateUpdateRequested> {
    private final Logger logger = LoggerFactory.getLogger(DesignAggregateUpdateRequestedInputMapper.class.getName());

    @Override
    public DesignAggregateUpdateRequested transform(InputMessage message) {
        if (!message.getValue().getType().equals(DesignAggregateUpdateRequested.TYPE)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), DesignAggregateUpdateRequested.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
