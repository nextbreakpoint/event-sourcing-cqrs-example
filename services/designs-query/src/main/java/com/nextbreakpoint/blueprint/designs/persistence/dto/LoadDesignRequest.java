package com.nextbreakpoint.blueprint.designs.persistence.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class LoadDesignRequest {
    private final UUID uuid;
    private final boolean draft;

    public LoadDesignRequest(UUID uuid, boolean draft) {
        this.uuid = Objects.requireNonNull(uuid);
        this.draft = draft;
    }
}
