package com.nextbreakpoint.blueprint.common.core;

import java.util.Objects;

public class Account {
    private String uuid;
    private String name;
    private String authorities;

    public Account(String uuid, String name, String authorities) {
        this.uuid = Objects.requireNonNull(uuid);
        this.name = Objects.requireNonNull(name);
        this.authorities = Objects.requireNonNull(authorities);
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getAuthorities() {
        return authorities;
    }
}
