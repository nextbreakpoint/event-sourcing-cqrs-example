package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Design {
    private final UUID evid;
    private final UUID uuid;
    private final long esid;
    private final String json;
    private final String checksum;
    private final String status;
    private final int levels;
    private final List<Tiles> tiles;
    private final String modified;

    @JsonCreator
    public Design(
        @JsonProperty("evid") UUID evid,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("esid") long esid,
        @JsonProperty("json") String json,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("status") String status,
        @JsonProperty("levels") int levels,
        @JsonProperty("tiles") List<Tiles> tiles,
        @JsonProperty("modified") String modified
    ) {
        this.evid = Objects.requireNonNull(evid);
        this.uuid = Objects.requireNonNull(uuid);
        this.esid = Objects.requireNonNull(esid);
        this.json = Objects.requireNonNull(json);
        this.checksum = Objects.requireNonNull(checksum);
        this.status = Objects.requireNonNull(status);
        this.levels = levels;
        this.tiles = Objects.requireNonNull(tiles);
        this.modified = Objects.requireNonNull(modified);
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

    public String getModified() {
        return modified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Design that = (Design) o;
        return getEsid() == that.getEsid() && getLevels() == that.getLevels() && Objects.equals(getEvid(), that.getEvid()) && Objects.equals(getUuid(), that.getUuid()) && Objects.equals(getJson(), that.getJson()) && Objects.equals(getChecksum(), that.getChecksum()) && Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getModified(), that.getModified()) && Objects.equals(getTiles(), that.getTiles());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEvid(), getUuid(), getEsid(), getJson(), getChecksum(), getStatus(), getLevels(), getModified(), getTiles());
    }

    @Override
    public String toString() {
        return "Design{" +
                "evid=" + evid +
                ", uuid=" + uuid +
                ", esid=" + esid +
                ", json='" + json + '\'' +
                ", checksum='" + checksum + '\'' +
                ", status='" + status + '\'' +
                ", levels=" + levels +
                ", updated=" + modified +
                ", tiles=" + tiles +
                '}';
    }
}
