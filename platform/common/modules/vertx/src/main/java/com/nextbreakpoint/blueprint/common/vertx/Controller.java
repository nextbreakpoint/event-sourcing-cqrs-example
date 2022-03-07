package com.nextbreakpoint.blueprint.common.vertx;

import rx.Single;

@FunctionalInterface
public interface Controller<T, R> {
    Single<R> onNext(T object);
}
