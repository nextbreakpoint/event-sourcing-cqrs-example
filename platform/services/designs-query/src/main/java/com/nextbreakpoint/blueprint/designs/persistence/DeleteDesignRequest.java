package com.nextbreakpoint.blueprint.designs.persistence;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignRequest {
    private final UUID uuid;

    public DeleteDesignRequest(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }
}
