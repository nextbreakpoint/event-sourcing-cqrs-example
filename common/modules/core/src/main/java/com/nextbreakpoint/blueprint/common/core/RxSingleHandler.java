package com.nextbreakpoint.blueprint.common.core;

import rx.Single;

@FunctionalInterface
public interface RxSingleHandler<T, R> {
    Single<R> handleSingle(T value);
}
