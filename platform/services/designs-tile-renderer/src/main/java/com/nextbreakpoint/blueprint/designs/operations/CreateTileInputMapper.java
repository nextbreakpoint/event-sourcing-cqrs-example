package com.nextbreakpoint.blueprint.designs.operations;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import com.nextbreakpoint.blueprint.designs.model.TileCreated;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class CreateTileInputMapper implements Mapper<RecordAndMessage, TileCreated> {
    private final Logger logger = LoggerFactory.getLogger(CreateTileInputMapper.class.getName());

    @Override
    public TileCreated transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.TILE_RENDER_REQUESTED)) {
            throw new IllegalArgumentException("Unexpected message type: " + input.getMessage().getMessageType());
        }
        try {
            return Json.decodeValue(input.getMessage().getMessageBody(), TileCreated.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
