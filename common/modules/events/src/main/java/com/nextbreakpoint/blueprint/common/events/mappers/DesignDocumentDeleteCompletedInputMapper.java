package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import io.vertx.core.json.DecodeException;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DesignDocumentDeleteCompletedInputMapper implements Mapper<InputMessage, DesignDocumentDeleteCompleted> {
    @Override
    public DesignDocumentDeleteCompleted transform(InputMessage message) {
        if (!message.getValue().getType().equals(DesignDocumentDeleteCompleted.TYPE)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), DesignDocumentDeleteCompleted.class);
        } catch (DecodeException e) {
            log.warn("Cannot decode message body: {}", message.getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
