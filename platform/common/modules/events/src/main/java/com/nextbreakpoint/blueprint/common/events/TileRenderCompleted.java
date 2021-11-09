package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TileRenderCompleted {
    private final UUID evid;
    private final UUID uuid;
    private final UUID esid;
    private final String checksum;
    private final int level;
    private final int row;
    private final int col;
    private final String status;

    @JsonCreator
    public TileRenderCompleted(
        @JsonProperty("evid") UUID evid,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("esid") UUID esid,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("level") int level,
        @JsonProperty("row") int row,
        @JsonProperty("col") int col,
        @JsonProperty("status") String status
    ) {
        this.evid = Objects.requireNonNull(evid);
        this.uuid = Objects.requireNonNull(uuid);
        this.esid = Objects.requireNonNull(esid);
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.row = row;
        this.col = col;
        this.status = Objects.requireNonNull(status);
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

    public String getStatus() {
        return status;
    }
}
