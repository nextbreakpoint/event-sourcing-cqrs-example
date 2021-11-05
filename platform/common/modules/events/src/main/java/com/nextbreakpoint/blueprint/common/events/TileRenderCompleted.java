package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class TileRenderCompleted {
    private final UUID uuid;
    private final UUID evid;
    private final String data;
    private final String checksum;
    private final int level;
    private final int row;
    private final int col;
    private final String status;

    @JsonCreator
    public TileRenderCompleted(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("evid") UUID evid,
            @JsonProperty("data") String data,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("level") int level,
            @JsonProperty("row") int row,
            @JsonProperty("col") int col,
            @JsonProperty("status") String status
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.evid = Objects.requireNonNull(evid);
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.row = row;
        this.col = col;
        this.status = Objects.requireNonNull(status);
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getEvid() {
        return evid;
    }

    public String getData() {
        return data;
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
