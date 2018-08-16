package com.nextbreakpoint.shop.designs.model;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignEvent {
    private final UUID uuid;

    public DeleteDesignEvent(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }
}
