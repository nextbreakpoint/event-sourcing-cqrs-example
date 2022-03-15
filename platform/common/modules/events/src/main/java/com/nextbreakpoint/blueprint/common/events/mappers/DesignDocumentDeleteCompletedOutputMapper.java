package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;

import java.util.Objects;
import java.util.UUID;

public class DesignDocumentDeleteCompletedOutputMapper implements MessageMapper<DesignDocumentDeleteCompleted, OutputMessage> {
    private final String messageSource;

    public DesignDocumentDeleteCompletedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignDocumentDeleteCompleted event) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignDocumentDeleteCompleted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
