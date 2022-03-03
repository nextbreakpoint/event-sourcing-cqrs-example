package com.nextbreakpoint.blueprint.designs.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Tile {
    private final int level;
    private final int col;
    private final int row;

    public Tile(int level, int row, int col) {
        this.level = level;
        this.row = row;
        this.col = col;
    }
}
