package com.nextbreakpoint.blueprint.common.vertx;

public interface MessageHandler<T, R> {
    void handleBlocking(T message);
}