package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class DesignAbortRequested {
    private final UUID uuid;
    private final Long timestamp;
    private final String checksum;

    @JsonCreator
    public DesignAbortRequested(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("checksum") String checksum
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.checksum = Objects.requireNonNull(checksum);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getChecksum() {
        return checksum;
    }
}
