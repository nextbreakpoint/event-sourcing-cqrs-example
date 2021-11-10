package com.nextbreakpoint.blueprint.designs.model;

import java.util.Objects;
import java.util.Set;

public class Tiles {
    private final int level;
    private final int requested;
    private final Set<Integer> completed;
    private final Set<Integer> failed;

    public Tiles(int level, int requested, Set<Integer> completed, Set<Integer> failed) {
        this.level = level;
        this.requested = requested;
        this.completed = completed;
        this.failed = failed;
    }

    public int getLevel() {
        return level;
    }

    public int getRequested() {
        return requested;
    }

    public Set<Integer> getCompleted() {
        return completed;
    }

    public Set<Integer> getFailed() {
        return failed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tiles tiles = (Tiles) o;
        return getLevel() == tiles.getLevel() && getRequested() == tiles.getRequested() && Objects.equals(getCompleted(), tiles.getCompleted()) && Objects.equals(getFailed(), tiles.getFailed());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLevel(), getRequested(), getCompleted(), getFailed());
    }
}
