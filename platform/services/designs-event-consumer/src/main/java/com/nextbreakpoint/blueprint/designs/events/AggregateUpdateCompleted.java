package com.nextbreakpoint.blueprint.designs.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class AggregateUpdateCompleted {
    private final UUID uuid;
    private final String data;
    private final String checksum;

    @JsonCreator
    public AggregateUpdateCompleted(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("data") String data,
        @JsonProperty("checksum") String checksum
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getData() {
        return data;
    }

    public String getChecksum() {
        return checksum;
    }
}
