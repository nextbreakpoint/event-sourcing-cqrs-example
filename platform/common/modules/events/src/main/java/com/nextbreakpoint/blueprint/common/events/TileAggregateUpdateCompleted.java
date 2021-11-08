package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateCompleted {
    private final UUID uuid;
    private final UUID evid;
    private final Long timestamp;

    @JsonCreator
    public TileAggregateUpdateCompleted(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("evid") UUID evid,
        @JsonProperty("timestamp") Long timestamp
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.evid = Objects.requireNonNull(evid);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getEvid() {
        return evid;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
