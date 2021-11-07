package com.nextbreakpoint.blueprint.designs.model;

import java.util.*;
import java.util.stream.Collectors;

public class DesignAccumulator {
    private UUID uuid;
    private UUID evid;
    private String json;
    private String status;
    private String checksum;
    private Date updated;
    private Map<Integer, Tiles> tiles;

    public DesignAccumulator(
        UUID uuid,
        UUID evid,
        String json,
        String status,
        String checksum,
        Date updated,
        Map<Integer, Tiles> tiles
    ) {
        this.uuid = uuid;
        this.evid = evid;
        this.json = json;
        this.status = status;
        this.checksum = checksum;
        this.updated = updated;
        this.tiles = tiles;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getEvid() {
        return evid;
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

    public Date getUpdated() {
        return updated;
    }

    public Map<Integer, Tiles> getTiles() {
        return tiles;
    }

    public Design toDesign() {
        final List<Tiles> tiles = this.tiles.values().stream()
                .sorted(Comparator.comparing(Tiles::getLevel))
                .collect(Collectors.toList());
        return new Design(uuid, evid, json, status, checksum, updated, tiles);
    }
}
