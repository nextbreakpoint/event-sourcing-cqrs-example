package com.nextbreakpoint.blueprint.designs.operations.render;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import com.nextbreakpoint.blueprint.designs.model.RenderCreated;
import com.nextbreakpoint.blueprint.designs.model.VersionCreated;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class RenderCreatedInputMapper implements Mapper<RecordAndMessage, RenderCreated> {
    private final Logger logger = LoggerFactory.getLogger(RenderCreatedInputMapper.class.getName());

    @Override
    public RenderCreated transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.DESIGN_RENDER_CREATED)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_RENDER_CREATED);
        }
        try {
            return Json.decodeValue(input.getMessage().getMessageBody(), RenderCreated.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
