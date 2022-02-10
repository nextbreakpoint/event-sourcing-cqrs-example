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
public class DesignDeleteRequested {
    public static final String TYPE = "design-delete-requested-v1";

    private final UUID eventId;
    private final UUID designId;

    @JsonCreator
    public DesignDeleteRequested(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("designId") UUID designId
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.designId = Objects.requireNonNull(designId);
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getDesignId() {
        return designId;
    }
}
