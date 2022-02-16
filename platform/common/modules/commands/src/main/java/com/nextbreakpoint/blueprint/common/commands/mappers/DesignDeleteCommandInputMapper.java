package com.nextbreakpoint.blueprint.common.commands.mappers;

import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.DecodeException;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

public class DesignDeleteCommandInputMapper implements Mapper<InputMessage, DesignDeleteCommand> {
    private final Logger logger = LoggerFactory.getLogger(DesignDeleteCommandInputMapper.class.getName());

    @Override
    public DesignDeleteCommand transform(InputMessage message) {
        if (!message.getValue().getType().equals(DesignDeleteCommand.TYPE)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), DesignDeleteCommand.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + message.getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
