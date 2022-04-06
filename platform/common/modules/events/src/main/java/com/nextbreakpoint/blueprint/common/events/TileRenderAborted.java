package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@JsonPropertyOrder({
        "designId",
        "revision",
        "checksum",
        "levels",
        "row",
        "col"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class TileRenderAborted {
    public static final String TYPE = "tile-render-aborted-v1";

    private final UUID designId;
    private final String revision;
    private final String checksum;
    private final int level;
    private final int row;
    private final int col;

    @JsonCreator
    public TileRenderAborted(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("revision") String revision,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("level") int level,
            @JsonProperty("row") int row,
            @JsonProperty("col") int col
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.revision = Objects.requireNonNull(revision);
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.row = row;
        this.col = col;
    }
}
