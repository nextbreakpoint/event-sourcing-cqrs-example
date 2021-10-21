package com.nextbreakpoint.blueprint.designs.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextbreakpoint.blueprint.designs.model.GenericEvent;

import java.util.Objects;
import java.util.UUID;

public class DesignInsertRequested extends GenericEvent {
    private final String json;

    @JsonCreator
    public DesignInsertRequested(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("json") String json,
        @JsonProperty("timestamp") Long timestamp
    ) {
        super(uuid, timestamp);
        this.json = Objects.requireNonNull(json);
    }

    public String getJson() {
        return json;
    }
}
