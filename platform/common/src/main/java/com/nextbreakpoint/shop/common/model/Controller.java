package com.nextbreakpoint.shop.common.model;

import rx.Single;

@FunctionalInterface
public interface Controller<T, R> {
    Single<R> onNext(T object);
}
