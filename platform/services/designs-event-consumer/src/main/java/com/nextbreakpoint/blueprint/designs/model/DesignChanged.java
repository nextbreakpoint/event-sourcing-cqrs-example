package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class DesignChanged {
    private final UUID uuid;
    private final String json;
    private final String checksum;
    private final Long timestamp;

    @JsonCreator
    public DesignChanged(@JsonProperty("uuid") UUID uuid,
                         @JsonProperty("json") String json,
                         @JsonProperty("checksum") String checksum,
                         @JsonProperty("timestamp") Long timestamp) {
        this.uuid = Objects.requireNonNull(uuid);
        this.json = Objects.requireNonNull(json);
        this.checksum = Objects.requireNonNull(checksum);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getJson() {
        return json;
    }

    public String getChecksum() {
        return checksum;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
