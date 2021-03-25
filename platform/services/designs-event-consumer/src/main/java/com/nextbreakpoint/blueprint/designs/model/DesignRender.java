package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class DesignRender {
    private final UUID uuid;
    private final Short level;
    private final DesignVersion version;

    @JsonCreator
    public DesignRender(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("level") Short level,
            @JsonProperty("version") DesignVersion version
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.level = Objects.requireNonNull(level);
        this.version = Objects.requireNonNull(version);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Short getLevel() {
        return level;
    }

    public DesignVersion getVersion() {
        return version;
    }
}
