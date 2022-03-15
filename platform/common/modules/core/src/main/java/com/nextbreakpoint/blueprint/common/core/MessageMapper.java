package com.nextbreakpoint.blueprint.common.core;

public interface MessageMapper<V, T> {
    T transform(V object, Tracing trace);
}
