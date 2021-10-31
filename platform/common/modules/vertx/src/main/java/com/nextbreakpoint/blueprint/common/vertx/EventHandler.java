package com.nextbreakpoint.blueprint.common.vertx;

import java.util.function.BiConsumer;

public interface EventHandler<T, R> {
    void handle(T message, BiConsumer<T, R> successHandler, BiConsumer<T, Throwable> failureHandler);
}