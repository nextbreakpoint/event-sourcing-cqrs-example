package com.nextbreakpoint.blueprint.common.core;

@FunctionalInterface
public interface Mapper<V, T> {
    T transform(V object);
}
