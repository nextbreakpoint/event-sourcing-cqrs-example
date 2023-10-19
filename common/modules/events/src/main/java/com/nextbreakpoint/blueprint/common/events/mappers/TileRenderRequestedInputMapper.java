package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import io.vertx.core.json.DecodeException;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TileRenderRequestedInputMapper implements Mapper<InputMessage, TileRenderRequested> {
    @Override
    public TileRenderRequested transform(InputMessage message) {
        if (!message.getValue().getType().equals(TileRenderRequested.TYPE)) {
            throw new IllegalArgumentException("Unexpected message type: " + message.getValue().getType());
        }
        try {
            return Json.decodeValue(message.getValue().getData(), TileRenderRequested.class);
        } catch (DecodeException e) {
            log.warn("Cannot decode message body: {}", message.getValue(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
