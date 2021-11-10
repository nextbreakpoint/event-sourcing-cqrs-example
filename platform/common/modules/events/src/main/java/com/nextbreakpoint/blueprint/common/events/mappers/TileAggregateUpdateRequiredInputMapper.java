package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class TileAggregateUpdateRequiredInputMapper implements Mapper<InputMessage, TileAggregateUpdateRequired> {
    private final Logger logger = LoggerFactory.getLogger(TileAggregateUpdateRequiredInputMapper.class.getName());

    @Override
    public TileAggregateUpdateRequired transform(InputMessage message) {
        if (!message.getValue().getType().equals(MessageType.TILE_AGGREGATE_UPDATE_REQUIRED)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), TileAggregateUpdateRequired.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
