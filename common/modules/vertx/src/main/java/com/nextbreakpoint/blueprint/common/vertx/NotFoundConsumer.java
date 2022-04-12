package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public class NotFoundConsumer<T> implements BiConsumer<RoutingContext, Optional<T>> {
    private BiConsumer<RoutingContext, T> consumer;

    public NotFoundConsumer(BiConsumer<RoutingContext, T> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
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
