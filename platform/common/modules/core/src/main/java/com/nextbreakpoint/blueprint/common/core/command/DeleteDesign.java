package com.nextbreakpoint.blueprint.common.core.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesign {
    private final UUID uuid;
    private final String timestamp;

    @JsonCreator
    public DeleteDesign(@JsonProperty("uuid") UUID uuid,
                        @JsonProperty("timestamp") String timestamp) {
        this.uuid = Objects.requireNonNull(uuid);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
