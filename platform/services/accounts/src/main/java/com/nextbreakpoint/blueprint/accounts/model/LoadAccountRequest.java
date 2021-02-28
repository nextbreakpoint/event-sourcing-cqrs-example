package com.nextbreakpoint.blueprint.accounts.model;

import java.util.Objects;
import java.util.UUID;

public class LoadAccountRequest {
    private final UUID uuid;

    public LoadAccountRequest(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }
}
