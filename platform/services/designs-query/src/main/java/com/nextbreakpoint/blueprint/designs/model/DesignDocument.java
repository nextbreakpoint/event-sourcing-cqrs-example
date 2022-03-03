package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDocument {
    private final UUID uuid;
    private final String json;
    private final String checksum;
    private final String revision;
    private final String modified;
    private final int levels;
    private final List<Tiles> tiles;

    @JsonCreator
    public DesignDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("json") String json,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("revision") String revision,
        @JsonProperty("modified") String modified,
        @JsonProperty("levels") int levels,
        @JsonProperty("tiles") List<Tiles> tiles
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.json = Objects.requireNonNull(json);
        this.checksum = Objects.requireNonNull(checksum);
        this.revision = Objects.requireNonNull(revision);
        this.modified = Objects.requireNonNull(modified);
        this.levels = levels;
        this.tiles = Objects.requireNonNull(tiles);
    }

    public static DesignDocument from(Design design) {
        return DesignDocument.builder()
                .withUuid(design.getDesignId())
                .withJson(design.getData())
                .withChecksum(design.getChecksum())
                .withRevision(design.getRevision())
                .withModified(design.getLastModified())
                .withLevels(design.getLevels())
                .withTiles(design.getTiles())
                .build();
    }
}
