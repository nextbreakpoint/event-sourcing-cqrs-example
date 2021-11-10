package com.nextbreakpoint.blueprint.designs.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Design {
    private final UUID evid;
    private final UUID uuid;
    private final UUID esid;
    private final String json;
    private final String status;
    private final String checksum;
    private final int levels;
    private final Date updated;
    private final List<Tiles> tiles;

    public Design(
        UUID evid,
        UUID uuid,
        UUID esid,
        String json,
        String status,
        String checksum,
        int levels,
        Date updated,
        List<Tiles> tiles
    ) {
        this.evid = Objects.requireNonNull(evid);
        this.uuid = Objects.requireNonNull(uuid);
        this.esid = Objects.requireNonNull(esid);
        this.json = Objects.requireNonNull(json);
        this.status = Objects.requireNonNull(status);
        this.checksum = Objects.requireNonNull(checksum);
        this.levels = levels;
        this.updated = Objects.requireNonNull(updated);
        this.tiles = Objects.requireNonNull(tiles);
    }

    public UUID getEvid() {
        return evid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getEsid() {
        return esid;
    }

    public String getJson() {
        return json;
    }

    public String getStatus() {
        return status;
    }

    public String getChecksum() {
        return checksum;
    }

    public int getLevels() {
        return levels;
    }

    public Date getUpdated() {
        return updated;
    }

    public List<Tiles> getTiles() {
        return tiles;
    }
}
