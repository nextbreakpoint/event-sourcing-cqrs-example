package com.nextbreakpoint.blueprint.designs.operations.update;

import java.util.Objects;
import java.util.UUID;

public class UpdateDesignResponse {
    private final UUID uuid;
    private final Integer result;

    public UpdateDesignResponse(UUID uuid, Integer result) {
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
