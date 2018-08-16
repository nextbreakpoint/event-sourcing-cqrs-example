package com.nextbreakpoint.shop.common;

@FunctionalInterface
public interface Mapper<V, T> {
    T transform(V object);
}
