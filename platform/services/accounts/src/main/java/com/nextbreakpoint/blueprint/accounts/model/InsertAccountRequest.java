package com.nextbreakpoint.blueprint.accounts.model;

import java.util.Objects;
import java.util.UUID;

public class InsertAccountRequest {
    private final UUID uuid;
    private final String name;
    private final String email;
    private final String role;

    public InsertAccountRequest(UUID uuid, String name, String email, String role) {
        this.uuid = Objects.requireNonNull(uuid);
        this.name = Objects.requireNonNull(name);
        this.role = Objects.requireNonNull(role);
        this.email = Objects.requireNonNull(email);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
