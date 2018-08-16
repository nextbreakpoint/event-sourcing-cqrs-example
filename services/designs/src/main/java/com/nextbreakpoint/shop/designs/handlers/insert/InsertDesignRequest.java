package com.nextbreakpoint.shop.designs.handlers.insert;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignRequest {
    private final UUID uuid;
    private final String json;

    public InsertDesignRequest(UUID uuid, String json) {
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
