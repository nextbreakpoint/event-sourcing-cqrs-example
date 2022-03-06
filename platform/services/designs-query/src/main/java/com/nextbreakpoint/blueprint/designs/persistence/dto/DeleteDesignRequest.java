package com.nextbreakpoint.blueprint.designs.persistence.dto;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignRequest {
    private final UUID uuid;
    private boolean draft;

    public DeleteDesignRequest(UUID uuid, boolean draft) {
        this.uuid = Objects.requireNonNull(uuid);
        this.draft = draft;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isDraft() {
        return draft;
    }
}
