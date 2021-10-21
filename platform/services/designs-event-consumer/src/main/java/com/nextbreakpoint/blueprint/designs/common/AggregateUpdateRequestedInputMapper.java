package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndEvent;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndMessage;
import com.nextbreakpoint.blueprint.designs.events.AggregateUpdateRequested;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;

public class AggregateUpdateRequestedInputMapper implements Mapper<RecordAndMessage, RecordAndEvent<AggregateUpdateRequested>> {
    private final Logger logger = LoggerFactory.getLogger(AggregateUpdateRequestedInputMapper.class.getName());

    @Override
    public RecordAndEvent<AggregateUpdateRequested> transform(RecordAndMessage input) {
        if (!input.getMessage().getMessageType().equals(MessageType.AGGREGATE_UPDATE_REQUESTED)) {
            throw new IllegalArgumentException("Unexpected message type: " + input.getMessage().getMessageType());
        }
        try {
            return new RecordAndEvent<>(input.getRecord(), Json.decodeValue(input.getMessage().getMessageBody(), AggregateUpdateRequested.class));
        } catch (DecodeException e) {
            logger.warn("Cannot decode message body: " + input.getMessage().getMessageBody(), e);
            throw new IllegalArgumentException("Message body cannot be decoded");
        }
    }
}
