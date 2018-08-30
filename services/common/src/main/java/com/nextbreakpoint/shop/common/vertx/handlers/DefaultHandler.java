package com.nextbreakpoint.shop.common.vertx.handlers;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import io.vertx.core.Handler;
import rx.Single;

import java.util.Objects;
import java.util.function.BiConsumer;

public class DefaultHandler<T, I, O, R> implements Handler<T> {
    private final Mapper<T, I> inputMapper;
    private final Mapper<O, R> outputMapper;
    private final Controller<I, O> controller;
    private final BiConsumer<T, R> successHandler;
    private final BiConsumer<T, Throwable> failureHandler;

    protected DefaultHandler(Mapper<T, I> inputMapper, Mapper<O, R> outputMapper, Controller<I, O> controller, BiConsumer<T, R> successHandler, BiConsumer<T, Throwable> failureHandler) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.controller = Objects.requireNonNull(controller);
        this.successHandler = Objects.requireNonNull(successHandler);
        this.failureHandler = Objects.requireNonNull(failureHandler);
    }

    @Override
    public void handle(T message) {
        Single.just(message)
                .map(inputMapper::transform)
                .flatMap(controller::onNext)
                .map(outputMapper::transform)
                .subscribe(result -> successHandler.accept(message, result), err -> failureHandler.accept(message, err));
    }

    public static <T, I, O, R> Builder<T, I, O, R> builder() {
        return new Builder<>();
    }

    public static class Builder<T, I, O, R> {
        private Mapper<T, I> inputMapper;
        private Mapper<O, R> outputMapper;
        private Controller<I, O> controller;
        private BiConsumer<T, R> successHandler;
        private BiConsumer<T, Throwable> failureHandler;

        public Builder() {}

        public Builder<T, I, O, R> withInputMapper(Mapper<T, I> mapper) {
            this.inputMapper = mapper;
            return this;
        }

        public Builder<T, I, O, R> withOutputMapper(Mapper<O, R> mapper) {
            this.outputMapper = mapper;
            return this;
        }

        public Builder<T, I, O, R> withController(Controller<I, O> controller) {
            this.controller = controller;
            return this;
        }

        public Builder<T, I, O, R> onSuccess(BiConsumer<T, R> consumer) {
            this.successHandler = consumer;
            return this;
        }

        public Builder<T, I, O, R> onFailure(BiConsumer<T, Throwable> consumer) {
            this.failureHandler = consumer;
            return this;
        }

        public DefaultHandler<T, I, O, R> build() {
            return new DefaultHandler<>(inputMapper, outputMapper, controller, successHandler, failureHandler);
        }
    }
}
