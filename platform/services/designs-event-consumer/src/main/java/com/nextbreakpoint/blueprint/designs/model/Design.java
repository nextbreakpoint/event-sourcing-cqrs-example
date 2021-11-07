package com.nextbreakpoint.blueprint.designs.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Design {
    private UUID uuid;
    private UUID evid;
    private String json;
    private String status;
    private String checksum;
    private Date updated;
    private List<Tiles> tiles;

    public Design(
        UUID uuid,
        UUID evid,
        String json,
        String status,
        String checksum,
        Date updated,
        List<Tiles> tiles
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.evid = Objects.requireNonNull(evid);
        this.json = Objects.requireNonNull(json);
        this.status = Objects.requireNonNull(status);
        this.checksum = Objects.requireNonNull(checksum);
        this.updated = Objects.requireNonNull(updated);
        this.tiles = Objects.requireNonNull(tiles);
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

    public List<Tiles> getTiles() {
        return tiles;
    }

    @Override
    public String toString() {
        return "Design{" +
                "uuid='" + uuid + '\'' +
                ", evid='" + evid + '\'' +
                ", json='" + json + '\'' +
                ", status='" + status + '\'' +
                ", checksum='" + checksum + '\'' +
                ", updated='" + updated + '\'' +
                ", tiles='" + tiles + '\'' +
                '}';
    }
}
