package com.nextbreakpoint.shop.designs.insert;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignResponse {
    private final UUID uuid;
    private final Integer result;

    public InsertDesignResponse(UUID uuid, Integer result) {
        this.uuid = Objects.requireNonNull(uuid);
        this.result = Objects.requireNonNull(result);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Integer getResult() {
        return result;
    }
}
