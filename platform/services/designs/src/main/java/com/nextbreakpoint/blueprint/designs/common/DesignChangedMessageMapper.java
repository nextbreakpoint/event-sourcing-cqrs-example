package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignChangedMessageMapper implements Mapper<DesignChanged, OutputMessage> {
    private final String messageSource;

    public DesignChangedMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignChanged event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                    UUID.randomUUID(),
                    MessageType.DESIGN_CHANGED,
                    Json.encode(event),
                    messageSource
            )
        );
    }
}
