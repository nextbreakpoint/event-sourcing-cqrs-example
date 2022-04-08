package com.nextbreakpoint.blueprint.common.core;

import lombok.Data;

import java.util.Objects;

@Data
public class Tombstone {
    private String key;

    public Tombstone(String key) {
        this.key = Objects.requireNonNull(key);
    }
}
