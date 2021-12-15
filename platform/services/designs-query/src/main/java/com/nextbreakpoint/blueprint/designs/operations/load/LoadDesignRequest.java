package com.nextbreakpoint.blueprint.designs.operations.load;

import java.util.Objects;
import java.util.UUID;

public class LoadDesignRequest {
    private final UUID uuid;

    public LoadDesignRequest(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }
}
