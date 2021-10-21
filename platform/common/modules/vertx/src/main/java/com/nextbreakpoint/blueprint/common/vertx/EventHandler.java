package com.nextbreakpoint.blueprint.common.vertx;

import java.util.function.BiConsumer;

public interface EventHandler<T> {
    void handle(T message, BiConsumer<T, Throwable> errorHandler);
}