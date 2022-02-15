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
    private final UUID eventId;
    private final UUID designId;
    private final long revision;
    private final String data;
    private final String checksum;
    private final String status;
    private final int levels;
    private final List<Tiles> tiles;
    private final String modified;

    @JsonCreator
    public Design(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("designId") UUID designId,
        @JsonProperty("revision") long revision,
        @JsonProperty("data") String data,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("status") String status,
        @JsonProperty("levels") int levels,
        @JsonProperty("tiles") List<Tiles> tiles,
        @JsonProperty("modified") String modified
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.designId = Objects.requireNonNull(designId);
        this.revision = revision;
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);
        this.status = Objects.requireNonNull(status);
        this.levels = levels;
        this.tiles = Objects.requireNonNull(tiles);
        this.modified = Objects.requireNonNull(modified);
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
        return getRevision() == that.getRevision() && getLevels() == that.getLevels() && Objects.equals(getEventId(), that.getEventId()) && Objects.equals(getDesignId(), that.getDesignId()) && Objects.equals(getData(), that.getData()) && Objects.equals(getChecksum(), that.getChecksum()) && Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getModified(), that.getModified()) && Objects.equals(getTiles(), that.getTiles());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEventId(), getDesignId(), getRevision(), getData(), getChecksum(), getStatus(), getLevels(), getModified(), getTiles());
    }

    @Override
    public String toString() {
        return "Design{" +
                "eventId=" + eventId +
                ", designId=" + designId +
                ", revision=" + revision +
                ", json='" + data + '\'' +
                ", checksum='" + checksum + '\'' +
                ", status='" + status + '\'' +
                ", levels=" + levels +
                ", tiles=" + tiles +
                ", modified=" + modified +
                '}';
    }
}
