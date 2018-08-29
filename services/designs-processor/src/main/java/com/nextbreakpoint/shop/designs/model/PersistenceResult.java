package com.nextbreakpoint.shop.designs.model;

import java.util.Objects;
import java.util.UUID;

public class PersistenceResult {
    private final UUID uuid;
    private final Integer records;

    public PersistenceResult(UUID uuid, Integer records) {
        this.uuid = Objects.requireNonNull(uuid);
        this.records = Objects.requireNonNull(records);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Integer getRecords() {
        return records;
    }
}
