package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;

import java.util.Objects;
import java.util.UUID;

public class DesignDocumentDeleteRequestedOutputMapper implements MessageMapper<DesignDocumentDeleteRequested, OutputMessage> {
    private final String messageSource;

    public DesignDocumentDeleteRequestedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignDocumentDeleteRequested event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignDocumentDeleteRequested.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
