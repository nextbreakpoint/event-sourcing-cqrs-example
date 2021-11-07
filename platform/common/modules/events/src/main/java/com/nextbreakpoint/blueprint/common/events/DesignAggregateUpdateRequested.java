package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateRequested {
    private final UUID uuid;
    private final Long timestamp;

    @JsonCreator
    public DesignAggregateUpdateRequested(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("timestamp") Long timestamp
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}