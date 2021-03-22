package com.nextbreakpoint.blueprint.designs.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PersistenceResult<T> {
    private final UUID uuid;
    private final UUID eventTimestamp;
    private final T value;

    public PersistenceResult(UUID uuid, UUID eventTimestamp, T value) {
        this.uuid = Objects.requireNonNull(uuid);
        this.eventTimestamp = Objects.requireNonNull(eventTimestamp);
        this.value = value;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getEventTimestamp() {
        return eventTimestamp;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }
}
