package com.nextbreakpoint.blueprint.common.commands.mappers;

import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.DecodeException;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DesignDeleteCommandInputMapper implements Mapper<InputMessage, DesignDeleteCommand> {
    @Override
    public DesignDeleteCommand transform(InputMessage message) {
        if (!message.getValue().getType().equals(DesignDeleteCommand.TYPE)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), DesignDeleteCommand.class);
        } catch (DecodeException e) {
            log.warn("Cannot decode message body: " + message.getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
