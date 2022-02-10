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
public class DesignUpdateRequested {
    public static final String TYPE = "design-update-requested-v1";

    private final UUID eventId;
    private final UUID designId;
    private final String data;
    private final int levels;

    @JsonCreator
    public DesignUpdateRequested(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("designId") UUID designId,
        @JsonProperty("data") String data,
        @JsonProperty("levels") int levels
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.designId = Objects.requireNonNull(designId);
        this.data = Objects.requireNonNull(data);
        this.levels = levels;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getDesignId() {
        return designId;
    }

    public String getData() {
        return data;
    }

    public int getLevels() {
        return levels;
    }
}
