package com.nextbreakpoint.shop.accounts.model;

import java.util.List;
import java.util.Objects;

public class ListAccountsResponse {
    private final List<String> uuids;

    public ListAccountsResponse(List<String> uuids) {
        this.uuids = Objects.requireNonNull(uuids);
    }

    public List<String> getUuids() {
        return uuids;
    }
}
