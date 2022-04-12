package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.function.BiConsumer;

public class ErrorConsumer implements BiConsumer<RoutingContext, Throwable> {
    @Override
    public void accept(RoutingContext context, Throwable error) {
        context.fail(Failure.requestFailed(error));
    }
}
