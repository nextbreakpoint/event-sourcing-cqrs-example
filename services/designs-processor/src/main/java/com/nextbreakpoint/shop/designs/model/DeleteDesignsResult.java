package com.nextbreakpoint.shop.designs.model;

import java.util.Objects;

public class DeleteDesignsResult {
    private final Integer result;

    public DeleteDesignsResult(Integer result) {
        this.result = Objects.requireNonNull(result);
    }

    public Integer getResult() {
        return result;
    }
}
