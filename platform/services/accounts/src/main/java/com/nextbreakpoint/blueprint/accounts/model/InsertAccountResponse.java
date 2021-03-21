package com.nextbreakpoint.blueprint.accounts.model;

import java.util.Objects;
import java.util.UUID;

public class InsertAccountResponse {
    private final UUID uuid;
    private final String authorities;
    private final Integer result;

    public InsertAccountResponse(UUID uuid, String authorities, Integer result) {
        this.uuid = Objects.requireNonNull(uuid);
        this.authorities = Objects.requireNonNull(authorities);
        this.result = Objects.requireNonNull(result);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Integer getResult() {
        return result;
    }

    public String getAuthorities() {
        return authorities;
    }
}
