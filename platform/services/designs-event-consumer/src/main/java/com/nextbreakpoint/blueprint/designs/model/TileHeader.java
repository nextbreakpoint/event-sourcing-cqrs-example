package com.nextbreakpoint.blueprint.designs.model;

import java.util.Objects;

public class TileHeader {
    private final Short level;
    private final Short x;
    private final Short y;

    public TileHeader(Short level, Short x, Short y) {
        this.level = Objects.requireNonNull(level);
        this.x = Objects.requireNonNull(x);
        this.y = Objects.requireNonNull(y);
    }

    public Short getLevel() {
        return level;
    }

    public Short getX() {
        return x;
    }

    public Short getY() {
        return y;
    }
}
