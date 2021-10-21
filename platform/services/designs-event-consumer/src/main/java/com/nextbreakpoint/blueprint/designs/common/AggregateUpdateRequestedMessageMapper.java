package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.events.AggregateUpdateRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class AggregateUpdateRequestedMessageMapper implements Mapper<AggregateUpdateRequested, Message> {
    private final String messageSource;

    public AggregateUpdateRequestedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(AggregateUpdateRequested event) {
        return new Message(
                UUID.randomUUID().toString(),
                MessageType.AGGREGATE_UPDATE_REQUESTED,
                Json.encode(event),
                messageSource,
                event.getUuid().toString(),
                System.currentTimeMillis()
        );
    }
}
