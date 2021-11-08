package com.nextbreakpoint.blueprint.common.core;

import java.util.Objects;

public class Tombstone {
    private String key;

    public Tombstone(String key) {
        this.key = Objects.requireNonNull(key);
    }

    public String getKey() {
        return key;
    }
}
