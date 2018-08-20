package com.nextbreakpoint.shop.designs.model;

import java.util.Objects;

public class DeleteDesignsResponse {
    private final Integer result;

    public DeleteDesignsResponse(Integer result) {
        this.result = Objects.requireNonNull(result);
    }

    public Integer getResult() {
        return result;
    }
}
