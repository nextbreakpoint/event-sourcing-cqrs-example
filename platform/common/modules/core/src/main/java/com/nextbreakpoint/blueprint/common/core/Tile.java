package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Tile {
    private final int level;
    private final int col;
    private final int row;

    @JsonCreator
    public Tile(
            @JsonProperty("level") int level,
            @JsonProperty("row") int row,
            @JsonProperty("col") int col
    ) {
        this.level = level;
        this.row = row;
        this.col = col;
    }
}
