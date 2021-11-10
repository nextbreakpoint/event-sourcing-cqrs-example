package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DesignAggregateUpdateCompletedInputMapper implements Mapper<InputMessage, DesignAggregateUpdateCompleted> {
    private final Logger logger = LoggerFactory.getLogger(DesignAggregateUpdateCompletedInputMapper.class.getName());

    @Override
    public DesignAggregateUpdateCompleted transform(InputMessage message) {
        if (!message.getValue().getType().equals(MessageType.DESIGN_AGGREGATE_UPDATE_COMPLETED)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), DesignAggregateUpdateCompleted.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
