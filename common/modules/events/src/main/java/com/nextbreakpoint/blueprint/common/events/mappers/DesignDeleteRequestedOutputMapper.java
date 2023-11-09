package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignDeleteRequestedOutputMapper implements Mapper<DesignDeleteRequested, OutputMessage> {
    private final String messageSource;

    public DesignDeleteRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignDeleteRequested event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignDeleteRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
