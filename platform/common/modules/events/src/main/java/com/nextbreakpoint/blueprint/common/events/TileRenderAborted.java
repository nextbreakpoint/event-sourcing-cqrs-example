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
public class TileRenderAborted {
    public static final String TYPE = "tile-render-aborted-v1";

    private final UUID eventId;
    private final UUID designId;
    private final long revision;
    private final String checksum;
    private final int level;
    private final int row;
    private final int col;

    @JsonCreator
    public TileRenderAborted(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("designId") UUID designId,
        @JsonProperty("revision") long revision,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("level") int level,
        @JsonProperty("row") int row,
        @JsonProperty("col") int col
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.designId = Objects.requireNonNull(designId);
        this.revision = revision;
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.row = row;
        this.col = col;
    }
}
