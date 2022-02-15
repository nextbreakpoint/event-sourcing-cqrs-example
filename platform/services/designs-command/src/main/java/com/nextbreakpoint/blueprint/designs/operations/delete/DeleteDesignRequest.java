package com.nextbreakpoint.blueprint.designs.operations.delete;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignRequest {
    private final UUID owner;
    private final UUID change;
    private final UUID uuid;

    public DeleteDesignRequest(UUID owner, UUID change, UUID uuid) {
        this.owner = Objects.requireNonNull(owner);
        this.change = Objects.requireNonNull(change);
        this.uuid = Objects.requireNonNull(uuid);
    }

    public UUID getOwner() {
        return owner;
    }

    public UUID getChange() {
        return change;
    }

    public UUID getUuid() {
        return uuid;
    }
}
