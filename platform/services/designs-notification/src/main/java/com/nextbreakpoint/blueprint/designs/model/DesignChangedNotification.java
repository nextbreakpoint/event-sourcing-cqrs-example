package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
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
