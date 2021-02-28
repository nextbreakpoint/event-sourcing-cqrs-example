package com.nextbreakpoint.blueprint.common.core;

import java.util.Objects;

public class Account {
    private String uuid;
    private String name;
    private String role;

    public Account(String uuid, String name, String role) {
        this.uuid = Objects.requireNonNull(uuid);
        this.name = Objects.requireNonNull(name);
        this.role = Objects.requireNonNull(role);
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
