package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAbortRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DesignAbortRequestedOutputMapper implements Mapper<DesignAbortRequested, OutputMessage> {
    private final String messageSource;

    public DesignAbortRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignAbortRequested event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                    UUID.randomUUID(),
                        DesignAbortRequested.TYPE,
                    Json.encode(event),
                    messageSource
                )
        );
    }
}
