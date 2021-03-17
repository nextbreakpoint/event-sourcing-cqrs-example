package com.nextbreakpoint.blueprint.common.core.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class UpdateDesign {
    private final UUID uuid;
    private final String json;
    private final String timestamp;

    @JsonCreator
    public UpdateDesign(@JsonProperty("uuid") UUID uuid,
                        @JsonProperty("json") String json,
                        @JsonProperty("timestamp") String timestamp) {
        this.uuid = Objects.requireNonNull(uuid);
        this.json = Objects.requireNonNull(json);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getJson() {
        return json;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
