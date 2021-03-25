package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class RenderCreated {
    private final UUID uuid;
    private final String data;
    private final String checksum;
    private final short level;

    @JsonCreator
    public RenderCreated(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("data") String data,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("level") short level
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
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

    public short getLevel() {
        return level;
    }
}
