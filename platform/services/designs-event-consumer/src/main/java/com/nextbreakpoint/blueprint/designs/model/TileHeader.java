package com.nextbreakpoint.blueprint.designs.model;

public class TileHeader {
    private final short level;
    private final short x;
    private final short y;

    public TileHeader(short level, short x, short y) {
        this.level = level;
        this.x = x;
        this.y = y;
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
}
