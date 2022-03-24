package com.nextbreakpoint.blueprint.designs.model;

import com.nextbreakpoint.blueprint.common.core.Tiles;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Level {
    private final int level;
    private final int requested;
    private final Set<Integer> completed;
    private final Set<Integer> failed;

    public Level(int level, int requested, Set<Integer> completed, Set<Integer> failed) {
        this.level = level;
        this.requested = requested;
        this.completed = completed;
        this.failed = failed;
    }

    public Tiles toTiles() {
        return Tiles.builder()
                .withLevel(getLevel())
                .withRequested(getRequested())
                .withCompleted(getCompleted().size())
                .withFailed(getFailed().size())
                .build();
    }

    public boolean isCompleted() {
        return (completed.size() + failed.size()) == requested;
    }
}
