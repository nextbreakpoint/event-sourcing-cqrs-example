package com.nextbreakpoint.shop.common;

import rx.Single;

@FunctionalInterface
public interface Controller<T, R> {
    Single<R> onNext(T object);
}
