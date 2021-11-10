package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class InsertDesignInputMapper implements Mapper<RecordAndMessage, InsertDesignCommand> {
    private final Logger logger = LoggerFactory.getLogger(InsertDesignInputMapper.class.getName());

    @Override
    public InsertDesignCommand transform(RecordAndMessage input) {
        if (!input.getMessage().getValue().getType().equals(MessageType.DESIGN_INSERT_REQUESTED)) {
            throw new IllegalArgumentException("Unexpected message type: " + input.getMessage().getValue().getType());
        }
        try {
            return Json.decodeValue(input.getMessage().getValue().getData(), InsertDesignCommand.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
