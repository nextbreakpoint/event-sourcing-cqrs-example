package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.nextbreakpoint.blueprint.common.core.Tile;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@JsonPropertyOrder({
        "designId",
        "commandId",
        "revision",
        "checksum",
        "data",
        "tiles"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class TilesRendered {
    public static final String TYPE = "tiles-rendered-v1";

    private final UUID designId;
    private final UUID commandId;
    private final String revision;
    private final String checksum;
    private final String data;
    private final List<Tile> tiles;

    @JsonCreator
    public TilesRendered(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("commandId") UUID commandId,
            @JsonProperty("revision") String revision,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("data") String data,
            @JsonProperty("tiles") List<Tile> tiles
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.commandId = Objects.requireNonNull(commandId);
        this.revision = Objects.requireNonNull(revision);
        this.checksum = Objects.requireNonNull(checksum);
        this.data = Objects.requireNonNull(data);
        this.tiles = Objects.requireNonNull(tiles);
    }
}
