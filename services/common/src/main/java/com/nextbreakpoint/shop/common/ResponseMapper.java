package com.nextbreakpoint.shop.common;

@FunctionalInterface
public interface ResponseMapper<T> {
    Result apply(T response);
}
