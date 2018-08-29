package com.nextbreakpoint.shop.common.model.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignCommand {
    private final UUID uuid;
    private final String json;
    private final Long timestamp;

    @JsonCreator
    public InsertDesignCommand(@JsonProperty("uuid") UUID uuid,
                               @JsonProperty("json") String json,
                               @JsonProperty("timestamp") Long timestamp) {
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

    public Long getTimestamp() {
        return timestamp;
    }
}
