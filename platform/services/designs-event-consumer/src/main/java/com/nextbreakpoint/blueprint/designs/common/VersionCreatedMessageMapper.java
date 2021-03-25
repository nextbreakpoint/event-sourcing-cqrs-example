package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.VersionCreated;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class VersionCreatedMessageMapper implements Mapper<VersionCreated, Message> {
    private final String messageSource;

    public VersionCreatedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(VersionCreated event) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_VERSION_CREATED, Json.encode(event), messageSource, event.getUuid().toString(), System.currentTimeMillis());
    }
}
