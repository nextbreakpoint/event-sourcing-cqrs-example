package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDocumentUpdateRequested {
    public static final String TYPE = "design-document-update-requested-v1";

    private final UUID eventId;
    private final UUID designId;
    private final long revision;
    private final String data;
    private final String checksum;
    private final String status;
    private final int levels;
    private final List<Tiles> tiles;
    private final LocalDateTime modified;

    @JsonCreator
    public DesignDocumentUpdateRequested(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("designId") UUID designId,
        @JsonProperty("revision") long revision,
        @JsonProperty("data") String data,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("status") String status,
        @JsonProperty("levels") int levels,
        @JsonProperty("tiles") List<Tiles> tiles,
        @JsonProperty("modified") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime modified
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

    public LocalDateTime getModified() {
        return modified;
    }

    @Data
    @Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
    public static class Tiles {
        private final int level;
        private final int requested;
        private final Set<Integer> completed;
        private final Set<Integer> failed;

        @JsonCreator
        public Tiles(
            @JsonProperty("level") int level,
            @JsonProperty("requested") int requested,
            @JsonProperty("completed") Set<Integer> completed,
            @JsonProperty("failed") Set<Integer> failed
        ) {
            this.level = level;
            this.requested = requested;
            this.completed = completed;
            this.failed = failed;
        }

        public int getLevel() {
            return level;
        }

        public int getRequested() {
            return requested;
        }

        public Set<Integer> getCompleted() {
            return completed;
        }

        public Set<Integer> getFailed() {
            return failed;
        }
    }
}
