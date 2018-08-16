package com.nextbreakpoint.shop.common;

import java.util.Objects;

public class Metadata {
    public static final String MODIFIED = "Modified";

    private final String name;
    private final String value;

    public Metadata(String name, String value) {
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
