package com.nextbreakpoint.blueprint.designs.operations.tile;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import com.nextbreakpoint.blueprint.designs.model.TileCompleted;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class TileCompletedInputMapper implements Mapper<RecordAndMessage, TileCompleted> {
    private final Logger logger = LoggerFactory.getLogger(TileCompletedInputMapper.class.getName());

    @Override
    public TileCompleted transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.DESIGN_TILE_COMPLETED)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_TILE_COMPLETED);
        }
        try {
            return Json.decodeValue(input.getMessage().getMessageBody(), TileCompleted.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
