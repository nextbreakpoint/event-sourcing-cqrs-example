package com.nextbreakpoint.blueprint.designs.model;

import com.nextbreakpoint.blueprint.common.core.Level;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final int levels;
    private final List<Level> tiles;
    private final LocalDateTime lastModified;

    public Design(
            UUID designId,
            UUID userId,
            UUID commandId,
            String data,
            String checksum,
            String revision,
            String status,
            int levels,
            List<Level> tiles,
            LocalDateTime lastModified
    ) {
        this.designId = designId;
        this.userId = userId;
        this.commandId = commandId;
        this.data = data;
        this.checksum = checksum;
        this.revision = revision;
        this.status = status;
        this.levels = levels;
        this.tiles = tiles;
        this.lastModified = lastModified;
    }
}
