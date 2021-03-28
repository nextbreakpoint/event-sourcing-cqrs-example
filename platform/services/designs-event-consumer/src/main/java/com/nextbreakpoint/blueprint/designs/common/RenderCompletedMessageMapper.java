package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.designs.model.RenderCompleted;
import com.nextbreakpoint.blueprint.designs.model.RenderCreated;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class RenderCompletedMessageMapper implements Mapper<RenderCompleted, Message> {
    private final String messageSource;

    public RenderCompletedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(RenderCompleted event) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGN_RENDER_COMPLETED, Json.encode(event), messageSource, event.getUuid().toString(), System.currentTimeMillis());
    }
}
