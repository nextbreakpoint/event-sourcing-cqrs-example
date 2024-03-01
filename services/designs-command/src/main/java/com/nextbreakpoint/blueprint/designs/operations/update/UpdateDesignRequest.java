package com.nextbreakpoint.blueprint.designs.operations.update;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class UpdateDesignRequest {
    private final UUID owner;
    private final UUID change;
    private final UUID uuid;
    private final String json;
    private final String token;
    private final Boolean published;

    public UpdateDesignRequest(UUID owner, UUID change, UUID uuid, String json, String token, Boolean published) {
        this.owner = Objects.requireNonNull(owner);
        this.change = Objects.requireNonNull(change);
        this.uuid = Objects.requireNonNull(uuid);
        this.json = Objects.requireNonNull(json);
        this.token = Objects.requireNonNull(token);
        this.published = Objects.requireNonNull(published);
    }
}
