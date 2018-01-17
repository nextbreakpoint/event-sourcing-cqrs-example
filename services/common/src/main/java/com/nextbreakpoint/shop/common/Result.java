package com.nextbreakpoint.shop.common;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Result {
    private final Set<Header> headers = new HashSet<>();
    private final String json;

    public Result() {
        this.json = null;
    }

    public Result(String json) {
        this.json = json;
    }

    public Result(String json, Set<Header> headers) {
        this.headers.addAll(headers);
        this.json = json;
    }

    public Set<Header> getHeaders() {
        return new HashSet<>(headers);
    }

    public Optional<String> getJson() {
        return Optional.ofNullable(json);
    }
}
