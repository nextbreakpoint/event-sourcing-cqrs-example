package com.nextbreakpoint.blueprint.designs;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Design {
    private final UUID evid;
    private final UUID uuid;
    private final long esid;
    private final String json;
    private final String checksum;
    private final String status;
    private final int levels;
    private final Date updated;
    private final List<Tiles> tiles;

    public Design(
        UUID evid,
        UUID uuid,
        long esid,
        String json,
        String checksum,
        String status,
        int levels,
        List<Tiles> tiles,
        Date updated
    ) {
        this.evid = Objects.requireNonNull(evid);
        this.uuid = Objects.requireNonNull(uuid);
        this.esid = Objects.requireNonNull(esid);
        this.json = Objects.requireNonNull(json);
        this.checksum = Objects.requireNonNull(checksum);
        this.status = Objects.requireNonNull(status);
        this.levels = levels;
        this.tiles = Objects.requireNonNull(tiles);
        this.updated = Objects.requireNonNull(updated);
    }

    public UUID getEvid() {
        return evid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getEsid() {
        return esid;
    }

    public String getJson() {
        return json;
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

    public List<Tiles> getTiles() {
        return tiles;
    }

    public Date getUpdated() {
        return updated;
    }
}
