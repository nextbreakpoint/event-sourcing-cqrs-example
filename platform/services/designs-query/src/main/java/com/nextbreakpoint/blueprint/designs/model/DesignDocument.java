package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDocument {
    private final UUID uuid;
    private final String json;
    private final String checksum;
    private final String modified;

    @JsonCreator
    public DesignDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("json") String json,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("modified") String modified
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.json = Objects.requireNonNull(json);
        this.checksum = Objects.requireNonNull(checksum);
        this.modified = Objects.requireNonNull(modified);
    }

    public static DesignDocument from(Design design) {
        return DesignDocument.builder()
                .withUuid(design.getDesignId())
                .withJson(design.getData())
                .withChecksum(design.getChecksum())
                .withModified(design.getLastModified())
                .build();
    }
}
