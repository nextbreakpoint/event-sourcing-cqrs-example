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
        "commandId",
        "revision",
        "checksum",
        "level",
        "row",
        "col"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class TileRenderCancelled {
    public static final String TYPE = "tile-render-cancelled-v1";

    private final UUID designId;
    private final UUID commandId;
    private final String revision;
    private final String checksum;
    private final int level;
    private final int row;
    private final int col;

    @JsonCreator
    public TileRenderCancelled(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("commandId") UUID commandId,
            @JsonProperty("revision") String revision,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("level") int level,
            @JsonProperty("row") int row,
            @JsonProperty("col") int col
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.commandId = Objects.requireNonNull(commandId);
        this.revision = Objects.requireNonNull(revision);
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.row = row;
        this.col = col;
    }
}
