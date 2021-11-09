package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class TileRenderAborted {
    private final UUID evid;
    private final UUID uuid;
    private final UUID esid;
    private final String checksum;
    private final int level;
    private final int row;
    private final int col;

    @JsonCreator
    public TileRenderAborted(
        @JsonProperty("evid") UUID evid,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("esid") UUID esid,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("level") int level,
        @JsonProperty("row") int row,
        @JsonProperty("col") int col
    ) {
        this.evid = Objects.requireNonNull(evid);
        this.uuid = Objects.requireNonNull(uuid);
        this.esid = Objects.requireNonNull(esid);
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.row = row;
        this.col = col;
    }

    public UUID getEvid() {
        return evid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getEsid() {
        return esid;
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
