package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class DesignTile {
    private final UUID uuid;
    private final Short level;
    private final Short x;
    private final Short y;
    private final DesignVersion version;

    @JsonCreator
    public DesignTile(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("level") Short level,
            @JsonProperty("level") Short x,
            @JsonProperty("level") Short y,
            @JsonProperty("version") DesignVersion version
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.level = Objects.requireNonNull(level);
        this.x = Objects.requireNonNull(x);
        this.y = Objects.requireNonNull(y);
        this.version = Objects.requireNonNull(version);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Short getLevel() {
        return level;
    }

    public Short getX() {
        return x;
    }

    public Short getY() {
        return y;
    }

    public DesignVersion getVersion() {
        return version;
    }
}
