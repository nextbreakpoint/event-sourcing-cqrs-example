package com.nextbreakpoint.blueprint.designs.operations.get;

import java.util.Objects;
import java.util.UUID;

public class GetTileRequest {
    private final UUID uuid;
    private final int level;
    private final int row;
    private final int col;
    private final int size;

    public GetTileRequest(UUID uuid, int level, int row, int col, int size) {
        this.uuid = Objects.requireNonNull(uuid);
        this.level = level;
        this.row = row;
        this.col = col;
        this.size = size;
    }

    public UUID getUuid() {
        return uuid;
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

    public int getSize() {
        return size;
    }
}
