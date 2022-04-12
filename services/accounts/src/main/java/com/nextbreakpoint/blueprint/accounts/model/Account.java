package com.nextbreakpoint.blueprint.accounts.model;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class Account {
    private String uuid;
    private String name;
    private String authorities;

    public Account(String uuid, String name, String authorities) {
        this.uuid = Objects.requireNonNull(uuid);
        this.name = Objects.requireNonNull(name);
        this.authorities = Objects.requireNonNull(authorities);
    }
}
