package com.nextbreakpoint.shop.common.model.events;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignEvent {
    private final UUID uuid;
    private final String json;

    public InsertDesignEvent(UUID uuid, String json) {
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
