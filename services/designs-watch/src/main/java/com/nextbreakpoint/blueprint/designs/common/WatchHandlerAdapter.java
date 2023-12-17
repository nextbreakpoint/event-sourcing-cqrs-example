package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.designs.handlers.WatchHandler;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;

public class WatchHandlerAdapter implements Handler<RoutingContext> {
    private final WatchHandler handler;

    public WatchHandlerAdapter(WatchHandler handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        handler.handle(new RoutingContextAdapter(routingContext));
    }
}
