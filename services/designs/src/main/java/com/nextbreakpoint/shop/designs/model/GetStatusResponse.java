package com.nextbreakpoint.shop.designs.model;

import com.nextbreakpoint.shop.designs.model.Status;

import java.util.Objects;

public class GetStatusResponse {
    private final Status status;

    public GetStatusResponse(Status status) {
        this.status = Objects.requireNonNull(status);
    }

    public Status getStatus() {
        return status;
    }
}
