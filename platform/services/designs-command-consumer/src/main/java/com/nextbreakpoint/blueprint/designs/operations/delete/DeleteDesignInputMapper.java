package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class DeleteDesignInputMapper implements Mapper<RecordAndMessage, DeleteDesignCommand> {
    private final Logger logger = LoggerFactory.getLogger(DeleteDesignInputMapper.class.getName());

    @Override
    public DeleteDesignCommand transform(RecordAndMessage input) {
        if (!input.getMessage().getType().equals(MessageType.DESIGN_DELETE_REQUESTED)) {
            throw new IllegalArgumentException("Unexpected message type: " + input.getMessage().getType());
        }
        try {
            return Json.decodeValue(input.getMessage().getBody(), DeleteDesignCommand.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getBody(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
