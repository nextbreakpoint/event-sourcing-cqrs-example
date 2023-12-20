package com.nextbreakpoint.blueprint.accounts.operations.insert;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class InsertAccountRequest {
    private final UUID uuid;
    private final String name;
    private final String email;
    private final String authorities;

    public InsertAccountRequest(UUID uuid, String name, String email, String authorities) {
        this.uuid = Objects.requireNonNull(uuid);
        this.name = Objects.requireNonNull(name);
        this.authorities = Objects.requireNonNull(authorities);
        this.email = Objects.requireNonNull(email);
    }
}
