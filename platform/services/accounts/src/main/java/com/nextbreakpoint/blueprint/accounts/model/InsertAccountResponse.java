package com.nextbreakpoint.blueprint.accounts.model;

import java.util.Objects;
import java.util.UUID;

public class InsertAccountResponse {
    private final UUID uuid;
    private final String role;
    private final Integer result;

    public InsertAccountResponse(UUID uuid, String role, Integer result) {
        this.uuid = Objects.requireNonNull(uuid);
        this.role = Objects.requireNonNull(role);
        this.result = Objects.requireNonNull(result);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Integer getResult() {
        return result;
    }

    public String getRole() {
        return role;
    }
}
