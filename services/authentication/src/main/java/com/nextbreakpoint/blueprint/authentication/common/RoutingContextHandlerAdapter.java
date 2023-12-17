package com.nextbreakpoint.blueprint.authentication.common;

import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;

public class RoutingContextHandlerAdapter implements Handler<RoutingContext> {
    private final String webUrl;
    private final Handler<RoutingContextAdapter> handler;

    public RoutingContextHandlerAdapter(String webUrl, Handler<RoutingContextAdapter> handler) {
        this.webUrl = Objects.requireNonNull(webUrl);
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        handler.handle(new RoutingContextAdapter(routingContext, webUrl));
    }
}
