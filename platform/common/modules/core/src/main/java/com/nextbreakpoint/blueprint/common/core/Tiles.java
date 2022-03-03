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
    private final int requested;
    private final int completed;
    private final int failed;

    @JsonCreator
    public Tiles(
        @JsonProperty("level") int level,
        @JsonProperty("requested") int requested,
        @JsonProperty("completed") int completed,
        @JsonProperty("failed") int failed
    ) {
        this.level = level;
        this.requested = requested;
        this.completed = completed;
        this.failed = failed;
    }
}
