package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.Set;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Tiles {
    private final int level;
    private final int requested;
    private final Set<Integer> completed;
    private final Set<Integer> failed;

    @JsonCreator
    public Tiles(
        @JsonProperty("level") int level,
        @JsonProperty("requested") int requested,
        @JsonProperty("completed") Set<Integer> completed,
        @JsonProperty("failed") Set<Integer> failed
    ) {
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

    @Override
    public String toString() {
        return "Tiles{" +
                "level=" + level +
                ", requested=" + requested +
                ", completed=" + completed +
                ", failed=" + failed +
                '}';
    }
}