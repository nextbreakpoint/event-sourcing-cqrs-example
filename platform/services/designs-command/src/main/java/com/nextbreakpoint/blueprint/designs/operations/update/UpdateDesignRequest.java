package com.nextbreakpoint.blueprint.designs.operations.update;

import java.util.Objects;
import java.util.UUID;

public class UpdateDesignRequest {
    private final UUID owner;
    private final UUID change;
    private final UUID uuid;
    private final String json;
    private final Boolean published;

    public UpdateDesignRequest(UUID owner, UUID change, UUID uuid, String json, Boolean published) {
        this.owner = Objects.requireNonNull(owner);
        this.change = Objects.requireNonNull(change);
        this.uuid = Objects.requireNonNull(uuid);
        this.json = Objects.requireNonNull(json);
        this.published = Objects.requireNonNull(published);
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

    public Boolean getPublished() {
        return published;
    }
}
