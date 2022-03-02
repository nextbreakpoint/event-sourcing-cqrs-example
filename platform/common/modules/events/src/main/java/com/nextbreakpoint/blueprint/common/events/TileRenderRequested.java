package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class TileRenderRequested {
    public static final String TYPE = "tile-render-requested-v1";

    private final UUID designId;
    private final String revision;
    private final String checksum;
    private final String data;
    private final int level;
    private final int row;
    private final int col;

    @JsonCreator
    public TileRenderRequested(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("revision") String revision,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("data") String data,
            @JsonProperty("level") int level,
            @JsonProperty("row") int row,
            @JsonProperty("col") int col
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.revision = Objects.requireNonNull(revision);
        this.checksum = Objects.requireNonNull(checksum);
        this.data = Objects.requireNonNull(data);
        this.level = level;
        this.row = row;
        this.col = col;
    }
}
