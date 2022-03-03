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

    private final UUID designId;
    private final UUID userId;
    private final UUID commandId;
    private final String data;
    private final String checksum;
    private final String revision;
    private final String status;
    private final int levels;
    private final List<Tiles> tiles;
    private final LocalDateTime modified;

    @JsonCreator
    public DesignDocumentUpdateRequested(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("commandId") UUID commandId,
            @JsonProperty("data") String data,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("revision") String revision,
            @JsonProperty("status") String status,
            @JsonProperty("levels") int levels,
            @JsonProperty("tiles") List<Tiles> tiles,
            @JsonProperty("modified") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime modified
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.userId = Objects.requireNonNull(userId);
        this.commandId = Objects.requireNonNull(commandId);
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);
        this.revision = Objects.requireNonNull(revision);
        this.status = Objects.requireNonNull(status);
        this.levels = levels;
        this.tiles = Objects.requireNonNull(tiles);
        this.modified = Objects.requireNonNull(modified);
    }

    @Data
    @Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
    public static class Tiles {
        private final int level;
        private final int requested;
        private final int completed;
        private final int failed;

        @JsonCreator
        public Tiles(
            @JsonProperty("level") int level,
            @JsonProperty("requested") int requested,
            @JsonProperty("completed") int completed,
            @JsonProperty("failed") int failed
        ) {
            this.level = level;
            this.requested = requested;
            this.completed = completed;
            this.failed = failed;
        }
    }
}
