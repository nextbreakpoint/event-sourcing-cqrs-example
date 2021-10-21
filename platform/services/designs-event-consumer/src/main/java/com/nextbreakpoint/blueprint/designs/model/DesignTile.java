package com.nextbreakpoint.blueprint.designs.model;

import java.util.Objects;

public class DesignTile {
    private final String checksum;
    private final short level;
    private final short x;
    private final short y;

    public DesignTile(
        String checksum,
        short level,
        short x,
        short y
    ) {
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.x = x;
        this.y = y;
    }

    public String getChecksum() {
        return checksum;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DesignTile that = (DesignTile) o;
        return getLevel() == that.getLevel() && getX() == that.getX() && getY() == that.getY() && Objects.equals(getChecksum(), that.getChecksum());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getChecksum(), getLevel(), getX(), getY());
    }

    @Override
    public String toString() {
        return "DesignTile{" +
                "checksum='" + checksum + '\'' +
                ", level=" + level +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
