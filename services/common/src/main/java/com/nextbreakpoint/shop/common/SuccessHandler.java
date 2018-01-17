package com.nextbreakpoint.shop.common;

import io.vertx.rxjava.ext.web.RoutingContext;

@FunctionalInterface
public interface SuccessHandler {
    void apply(RoutingContext context, Result result);
}
