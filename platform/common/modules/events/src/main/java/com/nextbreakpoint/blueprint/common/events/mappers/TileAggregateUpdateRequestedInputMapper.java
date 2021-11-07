package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequested;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class TileAggregateUpdateRequestedInputMapper implements Mapper<Message, TileAggregateUpdateRequested> {
    private final Logger logger = LoggerFactory.getLogger(TileAggregateUpdateRequestedInputMapper.class.getName());

    @Override
    public TileAggregateUpdateRequested transform(Message message) {
        if (!message.getType().equals(MessageType.TILE_AGGREGATE_UPDATE_REQUESTED)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getType());
        }
        try {
            return Json.decodeValue(message.getBody(), TileAggregateUpdateRequested.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getBody(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
