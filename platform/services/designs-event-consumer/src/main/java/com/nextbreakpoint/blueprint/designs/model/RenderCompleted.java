package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class RenderCompleted {
    private final UUID uuid;
    private final short level;

    @JsonCreator
    public RenderCompleted(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("level") short level
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.level = level;
    }

    public UUID getUuid() {
        return uuid;
    }

    public short getLevel() {
        return level;
    }
}
