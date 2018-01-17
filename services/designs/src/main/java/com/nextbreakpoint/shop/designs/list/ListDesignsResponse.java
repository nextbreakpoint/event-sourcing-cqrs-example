package com.nextbreakpoint.shop.designs.list;

import java.util.Objects;
import java.util.List;

public class ListDesignsResponse {
    private final String date;
    private final List<String> uuids;

    public ListDesignsResponse(String date, List<String> uuids) {
        this.date = Objects.requireNonNull(date);
        this.uuids = Objects.requireNonNull(uuids);
    }

    public String getDate() {
        return date;
    }

    public List<String> getUuids() {
        return uuids;
    }
}
