package com.nextbreakpoint.blueprint.designs.model;

import java.util.Set;

public class Tiles {
    private final int level;
    private final int requested;
    private final Set<String> completed;
    private final Set<String> failed;

    public Tiles(int level, int requested, Set<String> completed, Set<String> failed) {
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

    public Set<String> getCompleted() {
        return completed;
    }

    public Set<String> getFailed() {
        return failed;
    }
}
