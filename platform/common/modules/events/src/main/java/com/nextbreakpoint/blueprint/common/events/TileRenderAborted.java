package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class TileRenderAborted {
    private final UUID uuid;
    private final String checksum;
    private final int level;
    private final int row;
    private final int col;

    @JsonCreator
    public TileRenderAborted(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("level") int level,
        @JsonProperty("row") int row,
        @JsonProperty("col") int col
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.row = row;
        this.col = col;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getChecksum() {
        return checksum;
    }

    public int getLevel() {
        return level;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}
