package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateCompletedMessageMapper implements Mapper<DesignAggregateUpdateCompleted, Message> {
    private final String messageSource;

    public DesignAggregateUpdateCompletedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(DesignAggregateUpdateCompleted event) {
        return new Message(
                event.getUuid().toString(),
                0,
                System.currentTimeMillis(),
                new Payload(
                        UUID.randomUUID(),
                        MessageType.DESIGN_AGGREGATE_UPDATE_COMPLETED,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
