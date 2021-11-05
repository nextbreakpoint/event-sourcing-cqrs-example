package com.nextbreakpoint.blueprint.common.vertx;

public interface EventHandler<T, R> {
    void handleBlocking(T message);
}