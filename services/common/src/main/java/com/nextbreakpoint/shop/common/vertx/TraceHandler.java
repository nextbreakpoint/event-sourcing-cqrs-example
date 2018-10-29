package com.nextbreakpoint.shop.common.vertx;

import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class TraceHandler implements Handler<RoutingContext> {
    private TraceHandler() {}

    public static TraceHandler create() {
        return new TraceHandler();
    }

    @Override
    public void handle(RoutingContext context) {
        context.put("request-trace-id", UUID.randomUUID().toString());
        context.next();
    }
}
