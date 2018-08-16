package com.nextbreakpoint.shop.common;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Content {
    private final Set<Metadata> metadata = new HashSet<>();
    private final String json;

    public Content() {
        this.json = null;
    }

    public Content(String json) {
        this.json = json;
    }

    public Content(String json, Set<Metadata> metadata) {
        this.metadata.addAll(metadata);
        this.json = json;
    }

    public Set<Metadata> getMetadata() {
        return new HashSet<>(metadata);
    }

    public Optional<String> getJson() {
        return Optional.ofNullable(json);
    }
}
