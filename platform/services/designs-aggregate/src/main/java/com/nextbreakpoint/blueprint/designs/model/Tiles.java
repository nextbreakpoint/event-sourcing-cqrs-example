package com.nextbreakpoint.blueprint.designs.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
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
}
