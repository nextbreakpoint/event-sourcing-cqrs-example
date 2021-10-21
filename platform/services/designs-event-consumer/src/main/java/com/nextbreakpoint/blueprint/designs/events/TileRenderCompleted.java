package com.nextbreakpoint.blueprint.designs.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TileRenderCompleted {
    private final String checksum;
    private final short level;
    private final short x;
    private final short y;
    private final String status;

    @JsonCreator
    public TileRenderCompleted(
        @JsonProperty("checksum") String checksum,
        @JsonProperty("level") short level,
        @JsonProperty("x") short x,
        @JsonProperty("y") short y,
        @JsonProperty("status") String status
    ) {
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.x = x;
        this.y = y;
        this.status = Objects.requireNonNull(status);
    }

    public String getChecksum() {
        return checksum;
    }

    public short getLevel() {
        return level;
    }

    public short getX() {
        return x;
    }

    public short getY() {
        return y;
    }

    public String getStatus() {
        return status;
    }
}
