package com.nextbreakpoint.shop.common;

import io.vertx.rxjava.ext.web.RoutingContext;

public class FailedRequestHandler implements FailureHandler {
    @Override
    public void apply(RoutingContext context, Throwable error) {
        context.fail(Failure.requestFailed(error));
    }
}
