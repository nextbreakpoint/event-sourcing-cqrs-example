package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class TileRenderAbortedInputMapper implements Mapper<Message, TileRenderAborted> {
    private final Logger logger = LoggerFactory.getLogger(TileRenderAbortedInputMapper.class.getName());

    @Override
    public TileRenderAborted transform(Message message) {
        if (!message.getType().equals(MessageType.TILE_RENDER_ABORTED)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getType());
        }
        try {
            return Json.decodeValue(message.getBody(), TileRenderAborted.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getBody(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
