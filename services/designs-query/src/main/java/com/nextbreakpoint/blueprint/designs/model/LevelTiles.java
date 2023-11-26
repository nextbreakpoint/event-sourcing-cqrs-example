package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@JsonPropertyOrder({
    "level",
    "total",
    "completed"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class LevelTiles {
    private final int level;
    private final int total;
    private final int completed;

    @JsonCreator
    public LevelTiles(
        @JsonProperty("level") int level,
        @JsonProperty("total") int total,
        @JsonProperty("completed") int completed
    ) {
        this.level = level;
        this.total = total;
        this.completed = completed;
    }

    public static List<LevelTiles> getTiles(int levels, float completePercentage) {
        return IntStream.range(0, levels)
                .mapToObj(level -> makeTiles(level, completePercentage))
                .collect(Collectors.toList());
    }

    private static LevelTiles makeTiles(int level, float completePercentage) {
        final int total = (int) Math.rint(Math.pow(2, level * 2));
        final int completed = (int) Math.rint((completePercentage * total) / 100f);
        return new LevelTiles(level, total, completed);
    }
}
