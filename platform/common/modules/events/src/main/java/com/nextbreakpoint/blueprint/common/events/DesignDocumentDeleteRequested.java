package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDocumentDeleteRequested {
    public static final String TYPE = "design-document-delete-requested-v1";

    private final UUID eventId;
    private final UUID designId;
    private final long revision;

    @JsonCreator
    public DesignDocumentDeleteRequested(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("designId") UUID designId,
        @JsonProperty("revision") long revision
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.designId = Objects.requireNonNull(designId);
        this.revision = revision;
    }
}
