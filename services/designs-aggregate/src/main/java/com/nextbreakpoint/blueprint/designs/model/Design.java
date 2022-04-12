package com.nextbreakpoint.blueprint.designs.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
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
    private final ByteBuffer bitmap;
    private final LocalDateTime created;
    private final LocalDateTime updated;

    public Design(
            UUID designId,
            UUID userId,
            UUID commandId,
            String data,
            String checksum,
            String revision,
            String status,
            boolean published,
            int levels,
            ByteBuffer bitmap,
            LocalDateTime created,
            LocalDateTime updated
    ) {
        this.designId = designId;
        this.userId = userId;
        this.commandId = commandId;
        this.data = data;
        this.checksum = checksum;
        this.revision = revision;
        this.status = status;
        this.published = published;
        this.levels = levels;
        this.bitmap = bitmap;
        this.created = created;
        this.updated = updated;
    }
}
