package com.nextbreakpoint.shop.designs.model;

import java.util.Objects;

public class Status {
    private String name;
    private Long updated;

    public Status(String name, Long updated) {
        this.name = Objects.requireNonNull(name);
        this.updated = Objects.requireNonNull(updated);
    }

    public String getName() {
        return name;
    }

    public Long getUpdated() {
        return updated;
    }
}
