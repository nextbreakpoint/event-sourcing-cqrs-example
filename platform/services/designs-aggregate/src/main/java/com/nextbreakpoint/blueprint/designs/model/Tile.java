package com.nextbreakpoint.blueprint.designs.model;

public class Tile {
    private final int level;
    private final int col;
    private final int row;

    public Tile(int level, int row, int col) {
        this.level = level;
        this.row = row;
        this.col = col;
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
