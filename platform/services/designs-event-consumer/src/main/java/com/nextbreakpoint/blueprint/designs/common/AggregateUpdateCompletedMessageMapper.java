package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.events.AggregateUpdateCompleted;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class AggregateUpdateCompletedMessageMapper implements Mapper<AggregateUpdateCompleted, Message> {
    private final String messageSource;

    public AggregateUpdateCompletedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(AggregateUpdateCompleted event) {
        return new Message(
                UUID.randomUUID().toString(),
                MessageType.AGGREGATE_UPDATE_COMPLETED,
                Json.encode(event),
                messageSource,
                event.getUuid().toString(),
                System.currentTimeMillis()
        );
    }
}
