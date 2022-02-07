package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignAbortRequested;

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
                    Json.encodeValue(event),
                    messageSource
                )
        );
    }
}
