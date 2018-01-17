package com.nextbreakpoint.shop.common;

import java.util.Objects;

public class Header {
    private final String name;
    private final String value;

    public Header(String name, String value) {
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
