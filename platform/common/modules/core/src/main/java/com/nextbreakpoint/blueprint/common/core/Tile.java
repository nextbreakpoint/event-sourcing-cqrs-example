package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

@Data
@JsonPropertyOrder({
        "level",
        "row",
        "col"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Tile {
    private final int level;
    private final int row;
    private final int col;

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
