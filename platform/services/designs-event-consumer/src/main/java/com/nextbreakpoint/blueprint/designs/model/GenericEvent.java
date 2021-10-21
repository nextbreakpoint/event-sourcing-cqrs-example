package com.nextbreakpoint.blueprint.designs.model;

import java.util.Objects;
import java.util.UUID;

public abstract class GenericEvent {
    private final UUID uuid;
    private final Long timestamp;

    public GenericEvent(UUID uuid, Long timestamp) {
        this.uuid = Objects.requireNonNull(uuid);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
