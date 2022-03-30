package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextbreakpoint.blueprint.common.core.Tiles;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Design {
    private final UUID designId;
    private final UUID userId;
    private final UUID commandId;
    private final String data;
    private final String checksum;
    private final String revision;
    private final String status;
    private final boolean published;
    private final int levels;
    private final List<Tiles> tiles;
    private final String created;
    private final String updated;

    @JsonCreator
    public Design(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("commandId") UUID commandId,
            @JsonProperty("data") String data,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("revision") String revision,
            @JsonProperty("status") String status,
            @JsonProperty("published") boolean published,
            @JsonProperty("levels") int levels,
            @JsonProperty("tiles") List<Tiles> tiles,
            @JsonProperty("created") String created,
            @JsonProperty("updated") String updated
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.userId = Objects.requireNonNull(userId);
        this.commandId = Objects.requireNonNull(commandId);
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);
        this.revision = Objects.requireNonNull(revision);
        this.status = Objects.requireNonNull(status);
        this.published = published;
        this.levels = levels;
        this.tiles = Objects.requireNonNull(tiles);
        this.created = Objects.requireNonNull(created);
        this.updated = Objects.requireNonNull(updated);
    }
}
