package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class TileAggregateUpdateRequiredInputMapper implements Mapper<Message, TileAggregateUpdateRequired> {
    private final Logger logger = LoggerFactory.getLogger(TileAggregateUpdateRequiredInputMapper.class.getName());

    @Override
    public TileAggregateUpdateRequired transform(Message message) {
        if (!message.getType().equals(MessageType.TILE_AGGREGATE_UPDATE_REQUIRED)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getType());
        }
        try {
            return Json.decodeValue(message.getBody(), TileAggregateUpdateRequired.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getBody(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
