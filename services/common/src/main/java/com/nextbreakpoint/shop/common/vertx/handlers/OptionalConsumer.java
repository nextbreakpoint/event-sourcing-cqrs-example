package com.nextbreakpoint.shop.common.vertx.handlers;

import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Optional;
import java.util.function.BiConsumer;

public class OptionalConsumer<T> implements BiConsumer<RoutingContext, Optional<T>> {
    private BiConsumer<RoutingContext, T> consumer;

    public OptionalConsumer(BiConsumer<RoutingContext, T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(RoutingContext context, Optional<T> optional) {
        if (optional.isPresent()) {
            consumer.accept(context, optional.get());
        } else {
            context.response().setStatusCode(404).end();
        }
    }
}
