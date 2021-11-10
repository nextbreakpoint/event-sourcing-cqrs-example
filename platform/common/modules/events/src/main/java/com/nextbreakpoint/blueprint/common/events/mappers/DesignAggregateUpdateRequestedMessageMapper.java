package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateRequestedMessageMapper implements Mapper<DesignAggregateUpdateRequested, Message> {
    private final String messageSource;

    public DesignAggregateUpdateRequestedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(DesignAggregateUpdateRequested event) {
        return new Message(
                event.getUuid().toString(),
                0,
                System.currentTimeMillis(),
                new Payload(
                        UUID.randomUUID(),
                        MessageType.DESIGN_AGGREGATE_UPDATE_REQUESTED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
