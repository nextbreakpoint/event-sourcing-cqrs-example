package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;

import java.util.Objects;
import java.util.UUID;

public class DesignDocumentUpdateCompletedOutputMapper implements MessageMapper<DesignDocumentUpdateCompleted, OutputMessage> {
    private final String messageSource;

    public DesignDocumentUpdateCompletedOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(DesignDocumentUpdateCompleted event, Tracing trace) {
        return new OutputMessage(
                event.getDesignId().toString(),
                new Payload(
                        UUID.randomUUID(),
                        DesignDocumentUpdateCompleted.TYPE,
                        Json.encodeValue(event),
                        messageSource
                ),
                trace
        );
    }
}
