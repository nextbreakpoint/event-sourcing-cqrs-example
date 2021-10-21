package com.nextbreakpoint.blueprint.designs.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TileRenderRequested {
    private final String checksum;
    private final short level;
    private final short x;
    private final short y;
    private final String data;

    @JsonCreator
    public TileRenderRequested(
        @JsonProperty("checksum") String checksum,
        @JsonProperty("level") short level,
        @JsonProperty("x") short x,
        @JsonProperty("y") short y,
        @JsonProperty("data") String data
    ) {
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.x = x;
        this.y = y;
        this.data = Objects.requireNonNull(data);
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

    public String getData() {
        return data;
    }
}
