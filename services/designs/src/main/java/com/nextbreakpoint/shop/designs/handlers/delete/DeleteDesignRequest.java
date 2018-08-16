package com.nextbreakpoint.shop.designs.handlers.delete;

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
