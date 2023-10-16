package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.common.vertx.Failure;

public class FailureException extends RuntimeException {
    private Failure failure;

    public FailureException(Failure failure) {
        this.failure = failure;
    }

    public Failure getFailure() {
        return failure;
    }
}
