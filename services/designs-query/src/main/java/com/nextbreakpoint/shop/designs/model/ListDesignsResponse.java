package com.nextbreakpoint.shop.designs.model;

import java.util.List;
import java.util.Objects;

public class ListDesignsResponse {
    private final Long updated;
    private final List<String> uuids;

    public ListDesignsResponse(Long updated, List<String> uuids) {
        this.updated = Objects.requireNonNull(updated);
        this.uuids = Objects.requireNonNull(uuids);
    }

    public Long getUpdated() {
        return updated;
    }

    public List<String> getUuids() {
        return uuids;
    }
}
