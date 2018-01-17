package com.nextbreakpoint.shop.designs.model;

import java.util.Objects;

public class Status {
    private String name;
    private String date;

    public Status(String name, String date) {
        this.name = Objects.requireNonNull(name);
        this.date = Objects.requireNonNull(date);
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }
}
