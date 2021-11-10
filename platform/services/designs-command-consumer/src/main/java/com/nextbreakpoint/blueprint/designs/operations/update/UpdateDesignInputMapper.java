package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class UpdateDesignInputMapper implements Mapper<RecordAndMessage, UpdateDesignCommand> {
    private final Logger logger = LoggerFactory.getLogger(UpdateDesignInputMapper.class.getName());

    @Override
    public UpdateDesignCommand transform(RecordAndMessage input) {
        if (!input.getMessage().getPayload().getType().equals(MessageType.DESIGN_UPDATE_REQUESTED)) {
            throw new IllegalArgumentException("Unexpected message type: " + input.getMessage().getPayload().getType());
        }
        try {
            return Json.decodeValue(input.getMessage().getPayload().getData(), UpdateDesignCommand.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getPayload(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
