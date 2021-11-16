package com.nextbreakpoint.blueprint.common.core;

@FunctionalInterface
public interface BlockingHandler<T> {
    void handleBlocking(T message);
}
