package com.nextbreakpoint.shop.common;

import io.vertx.rxjava.ext.web.RoutingContext;

@FunctionalInterface
public interface RequestMapper<T> {
    T apply(RoutingContext context);
}
