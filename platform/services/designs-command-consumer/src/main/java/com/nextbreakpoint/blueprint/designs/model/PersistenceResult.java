package com.nextbreakpoint.blueprint.designs.model;

import java.util.Objects;
import java.util.UUID;

public class PersistenceResult {
    private final UUID uuid;

    public PersistenceResult(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }
}
