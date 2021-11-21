package com.nextbreakpoint.blueprint.designs.model;

import java.util.*;
import java.util.stream.Collectors;

public class DesignAccumulator {
    private UUID evid;
    private UUID uuid;
    private long esid;
    private String json;
    private String checksum;
    private String status;
    private int levels;
    private Map<Integer, Tiles> tiles;
    private Date updated;

    public DesignAccumulator(
        UUID evid,
        UUID uuid,
        long esid,
        String json,
        String checksum,
        String status,
        int levels,
        Map<Integer, Tiles> tiles,
        Date updated
    ) {
        this.evid = evid;
        this.uuid = uuid;
        this.esid = esid;
        this.json = json;
        this.checksum = checksum;
        this.status = status;
        this.levels = levels;
        this.tiles = tiles;
        this.updated = updated;
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

    public Date getUpdated() {
        return updated;
    }

    public Design toDesign() {
        return new Design(evid, uuid, esid, json, checksum, status, levels, getTilesList(), updated);
    }

    private List<Tiles> getTilesList() {
        if (tiles == null) {
            return new ArrayList<>();
        }
        return this.tiles.values().stream()
            .sorted(Comparator.comparing(Tiles::getLevel))
            .collect(Collectors.toList());
    }
}
