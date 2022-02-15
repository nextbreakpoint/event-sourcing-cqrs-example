package com.nextbreakpoint.blueprint.designs.operations.insert;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignRequest {
    private final UUID owner;
    private final UUID change;
    private final UUID uuid;
    private final String json;
    private final int levels;

    public InsertDesignRequest(UUID owner, UUID change, UUID uuid, String json, int levels) {
        this.owner = Objects.requireNonNull(owner);
        this.change = Objects.requireNonNull(change);
        this.uuid = Objects.requireNonNull(uuid);
        this.json = Objects.requireNonNull(json);
        if (levels < 0 || levels > 8) {
            throw new IllegalArgumentException("levels must be a number >= 0 and <= 8");
        }
        this.levels = levels;
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

    public String getJson() {
        return json;
    }

    public int getLevels() {
        return levels;
    }
}
