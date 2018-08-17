package com.nextbreakpoint.shop.designs.update;

import java.util.Objects;
import java.util.UUID;

public class UpdateDesignRequest {
    private final UUID uuid;
    private final String json;

    public UpdateDesignRequest(UUID uuid, String json) {
        this.uuid = Objects.requireNonNull(uuid);
        this.json = Objects.requireNonNull(json);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getJson() {
        return json;
    }
}
