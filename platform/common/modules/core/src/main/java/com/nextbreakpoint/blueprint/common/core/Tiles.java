package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Tiles {
    private final int level;
    private final int completed;

    @JsonCreator
    public Tiles(
        @JsonProperty("level") int level,
        @JsonProperty("completed") int completed
    ) {
        this.level = level;
        this.completed = completed;
    }
}
