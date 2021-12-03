package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignInsertRequestedOutputMapper implements Mapper<DesignInsertRequested, OutputMessage> {
    private final String messageSource;

    public DesignInsertRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignInsertRequested event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignInsertRequested.TYPE,
                        Json.encode(event),
                        messageSource
                )
        );
    }
}
