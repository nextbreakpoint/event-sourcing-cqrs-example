package com.nextbreakpoint.blueprint.designs.common;

import lombok.Getter;

@Getter
class TileParams {
    private final int level;
    private final int row;
    private final int col;
    private final int size;

    public TileParams(int level, int row, int col, int size) {
        this.level = level;
        this.col = col;
        this.row = row;
        this.size = size;
    }
}
