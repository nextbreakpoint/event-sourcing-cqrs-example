package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignDocumentDeleteRequestedOutputMapper implements Mapper<DesignDocumentDeleteRequested, OutputMessage> {
    private final String messageSource;

    public DesignDocumentDeleteRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignDocumentDeleteRequested event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignDocumentDeleteRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
