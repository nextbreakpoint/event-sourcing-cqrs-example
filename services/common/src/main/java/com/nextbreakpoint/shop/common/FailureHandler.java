package com.nextbreakpoint.shop.common;

import io.vertx.rxjava.ext.web.RoutingContext;

@FunctionalInterface
public interface FailureHandler {
    void apply(RoutingContext context, Throwable error);
}
