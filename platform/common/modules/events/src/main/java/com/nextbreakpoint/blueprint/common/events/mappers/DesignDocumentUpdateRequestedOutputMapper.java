package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignDocumentUpdateRequestedOutputMapper implements Mapper<DesignDocumentUpdateRequested, OutputMessage> {
    private final String messageSource;

    public DesignDocumentUpdateRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignDocumentUpdateRequested event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignDocumentUpdateRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
