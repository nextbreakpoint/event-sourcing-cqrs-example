package com.nextbreakpoint.blueprint.accounts.operations.list;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class ListAccountsResponse {
    private final List<String> uuids;

    public ListAccountsResponse(List<String> uuids) {
        this.uuids = Objects.requireNonNull(uuids);
    }
}
