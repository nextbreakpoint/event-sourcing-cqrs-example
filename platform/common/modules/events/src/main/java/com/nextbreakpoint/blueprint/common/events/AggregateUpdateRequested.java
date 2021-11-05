package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class AggregateUpdateRequested {
    private final UUID uuid;
    private final Long timestamp;
    private final String status;

    @JsonCreator
    public AggregateUpdateRequested(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("status") String status
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.status = status;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }
}
