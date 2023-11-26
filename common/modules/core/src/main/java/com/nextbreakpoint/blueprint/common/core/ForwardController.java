package com.nextbreakpoint.blueprint.common.core;

import rx.Single;

import java.util.Objects;

public class ForwardController<T> implements Controller<InputMessage<T>, Void> {
    private final Mapper<InputMessage<T>, OutputMessage<T>> mapper;
    private final MessageEmitter<T> emitter;

    public ForwardController(Mapper<InputMessage<T>, OutputMessage<T>> mapper, MessageEmitter<T> emitter) {
        this.mapper = Objects.requireNonNull(mapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage<T> message) {
        return Single.just(message)
                .map(mapper::transform)
                .flatMap(emitter::send);
    }
}
