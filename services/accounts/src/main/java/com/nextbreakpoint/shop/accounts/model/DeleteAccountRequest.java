package com.nextbreakpoint.shop.accounts.model;

import java.util.Objects;
import java.util.UUID;

public class DeleteAccountRequest {
    private final UUID uuid;

    public DeleteAccountRequest(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }
}
