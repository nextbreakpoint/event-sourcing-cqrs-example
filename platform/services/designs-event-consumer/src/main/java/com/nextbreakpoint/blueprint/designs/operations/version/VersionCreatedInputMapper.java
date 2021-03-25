package com.nextbreakpoint.blueprint.designs.operations.version;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import com.nextbreakpoint.blueprint.designs.model.VersionCreated;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class VersionCreatedInputMapper implements Mapper<RecordAndMessage, VersionCreated> {
    private final Logger logger = LoggerFactory.getLogger(VersionCreatedInputMapper.class.getName());

    @Override
    public VersionCreated transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.DESIGN_VERSION_CREATED)) {
            throw new IllegalArgumentException("message type must be " + MessageType.DESIGN_VERSION_CREATED);
        }
        try {
            return Json.decodeValue(input.getMessage().getMessageBody(), VersionCreated.class);
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("message body cannot be decoded");
        }
    }
}
