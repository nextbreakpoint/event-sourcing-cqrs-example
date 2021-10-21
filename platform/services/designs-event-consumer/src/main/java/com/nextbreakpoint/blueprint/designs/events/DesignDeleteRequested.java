package com.nextbreakpoint.blueprint.designs.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextbreakpoint.blueprint.designs.model.GenericEvent;

import java.util.UUID;

public class DesignDeleteRequested extends GenericEvent {
    @JsonCreator
    public DesignDeleteRequested(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("timestamp") Long timestamp
    ) {
        super(uuid, timestamp);
    }
}
