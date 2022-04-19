package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.nextbreakpoint.blueprint.common.core.Tiles;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@JsonPropertyOrder({
        "uuid",
        "json",
        "checksum",
        "revision",
        "created",
        "updated",
        "published",
        "levels",
        "tiles"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDocument {
    private final UUID uuid;
    private final String json;
    private final String checksum;
    private final String revision;
    private final String created;
    private final String updated;
    private final boolean published;
    private final int levels;
    private final List<Tiles> tiles;

    @JsonCreator
    public DesignDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("json") String json,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("revision") String revision,
        @JsonProperty("created") String created,
        @JsonProperty("updated") String updated,
        @JsonProperty("published") boolean published,
        @JsonProperty("levels") int levels,
        @JsonProperty("tiles") List<Tiles> tiles
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.json = Objects.requireNonNull(json);
        this.checksum = Objects.requireNonNull(checksum);
        this.revision = Objects.requireNonNull(revision);
        this.created = Objects.requireNonNull(created);
        this.updated = Objects.requireNonNull(updated);
        this.published = published;
        this.levels = levels;
        this.tiles = Objects.requireNonNull(tiles);
    }

    public static DesignDocument from(Design design) {
        return DesignDocument.builder()
                .withUuid(design.getDesignId())
                .withJson(design.getData())
                .withChecksum(design.getChecksum())
                .withRevision(design.getRevision())
                .withCreated(design.getCreated())
                .withUpdated(design.getUpdated())
                .withPublished(design.isPublished())
                .withLevels(design.getLevels())
                .withTiles(design.getTiles())
                .build();
    }
}
