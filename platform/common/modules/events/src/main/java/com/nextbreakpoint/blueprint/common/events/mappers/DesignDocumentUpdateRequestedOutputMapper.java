package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignDocumentUpdateRequestedOutputMapper implements MessageMapper<DesignDocumentUpdateRequested, OutputMessage> {
    private final String messageSource;

    public DesignDocumentUpdateRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignDocumentUpdateRequested event, Tracing trace) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignDocumentUpdateRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
