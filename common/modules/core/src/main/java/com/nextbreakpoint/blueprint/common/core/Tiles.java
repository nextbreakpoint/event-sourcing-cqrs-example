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
    "total",
    "completed"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Tiles {
    private final int level;
    private final int total;
    private final int completed;

    @JsonCreator
    public Tiles(
        @JsonProperty("level") int level,
        @JsonProperty("total") int total,
        @JsonProperty("completed") int completed
    ) {
        this.level = level;
        this.total = total;
        this.completed = completed;
    }
}
