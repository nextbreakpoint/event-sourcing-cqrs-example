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
public class TileRenderCompleted {
    public static final String TYPE = "tile-render-completed-v1";

    private final UUID eventId;
    private final UUID designId;
    private final long revision;
    private final String checksum;
    private final int level;
    private final int row;
    private final int col;
    private final String status;

    @JsonCreator
    public TileRenderCompleted(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("designId") UUID designId,
        @JsonProperty("revision") long revision,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("level") int level,
        @JsonProperty("row") int row,
        @JsonProperty("col") int col,
        @JsonProperty("status") String status
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.designId = Objects.requireNonNull(designId);
        this.revision = revision;
        this.checksum = Objects.requireNonNull(checksum);
        this.level = level;
        this.row = row;
        this.col = col;
        this.status = Objects.requireNonNull(status);
    }
}
