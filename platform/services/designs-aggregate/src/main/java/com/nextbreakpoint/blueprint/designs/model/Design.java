package com.nextbreakpoint.blueprint.designs.model;

import java.util.*;

public class Design {
    private final UUID evid;
    private final UUID uuid;
    private final long esid;
    private final String json;
    private final String checksum;
    private final String status;
    private final int levels;
    private final Map<Integer, Tiles> tiles;
    private final Date modified;

    public Design(
        UUID evid,
        UUID uuid,
        long esid,
        String json,
        String checksum,
        String status,
        int levels,
        Map<Integer, Tiles> tiles,
        Date modified
    ) {
        this.evid = evid;
        this.uuid = uuid;
        this.esid = esid;
        this.json = json;
        this.checksum = checksum;
        this.status = status;
        this.levels = levels;
        this.tiles = tiles;
        this.modified = modified;
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

    public Map<Integer, Tiles> getTiles() {
        return tiles;
    }

    public Date getModified() {
        return modified;
    }
}
