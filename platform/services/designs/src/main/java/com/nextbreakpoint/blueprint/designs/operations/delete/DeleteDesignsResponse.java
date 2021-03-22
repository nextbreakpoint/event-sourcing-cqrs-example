package com.nextbreakpoint.blueprint.designs.operations.delete;

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
