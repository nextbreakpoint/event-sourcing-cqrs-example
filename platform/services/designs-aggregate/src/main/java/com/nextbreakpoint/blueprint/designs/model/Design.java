package com.nextbreakpoint.blueprint.designs.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class Design {
    private final UUID eventId;
    private final UUID designId;
    private final long revision;
    private final String data;
    private final String checksum;
    private final String status;
    private final int levels;
    private final Map<Integer, Tiles> tiles;
    private final LocalDateTime modified;

    public Design(
        UUID eventId,
        UUID designId,
        long revision,
        String data,
        String checksum,
        String status,
        int levels,
        Map<Integer, Tiles> tiles,
        LocalDateTime modified
    ) {
        this.eventId = eventId;
        this.designId = designId;
        this.revision = revision;
        this.data = data;
        this.checksum = checksum;
        this.status = status;
        this.levels = levels;
        this.tiles = tiles;
        this.modified = modified;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getDesignId() {
        return designId;
    }

    public long getRevision() {
        return revision;
    }

    public String getData() {
        return data;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getStatus() {
        return status;
    }

    public int getLevels() {
        return levels;
    }

    public Map<Integer, Tiles> getTiles() {
        return tiles;
    }

    public LocalDateTime getModified() {
        return modified;
    }
}
