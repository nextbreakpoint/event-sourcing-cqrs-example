package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class DesignChangedNotification {
    private final String key;
    private final long timestamp;

    @JsonCreator
    public DesignChangedNotification(
        @JsonProperty("key") String key,
        @JsonProperty("timestamp") long timestamp
    ) {
        this.key = Objects.requireNonNull(key);
        this.timestamp = timestamp;
    }

    public String getKey() {
        return key;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
