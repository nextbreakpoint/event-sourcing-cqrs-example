package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;

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
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignInsertRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
