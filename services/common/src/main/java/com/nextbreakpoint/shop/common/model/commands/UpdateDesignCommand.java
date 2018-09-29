package com.nextbreakpoint.shop.common.model.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class UpdateDesignCommand {
    private final UUID uuid;
    private final String json;
    private final UUID timestamp;

    @JsonCreator
    public UpdateDesignCommand(@JsonProperty("uuid") UUID uuid,
                               @JsonProperty("json") String json,
                               @JsonProperty("timestamp") UUID timestamp) {
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

    public UUID getTimestamp() {
        return timestamp;
    }
}
