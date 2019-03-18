package com.nextbreakpoint.shop.common.model;

@FunctionalInterface
public interface Mapper<V, T> {
    T transform(V object);
}
