package com.nextbreakpoint.blueprint.common.core;

import rx.Single;

@FunctionalInterface
public interface Controller<T, R> {
    Single<R> onNext(T object);
}
