package com.nextbreakpoint.shop.common;

import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;

import static rx.Single.fromCallable;

public class RESTContentHandler<I, O> implements Handler<RoutingContext> {
    private final Mapper<RoutingContext, I> inputMapper;
    private final Mapper<O, Content> outputMapper;
    private final Controller<I, O> controller;
    private final SuccessHandler successHandler;
    private final FailureHandler failureHandler;

    private RESTContentHandler(Mapper<RoutingContext, I> inputMapper, Mapper<O, Content> outputMapper, Controller<I, O> controller, SuccessHandler successHandler, FailureHandler failureHandler) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.controller = Objects.requireNonNull(controller);
        this.successHandler = Objects.requireNonNull(successHandler);
        this.failureHandler = Objects.requireNonNull(failureHandler);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        fromCallable(() -> inputMapper.transform(routingContext))
                .flatMap(controller::onNext)
                .map(outputMapper::transform)
                .subscribe(result -> successHandler.apply(routingContext, result), err -> failureHandler.apply(routingContext, err));
    }

    public static <I, O> Builder<I, O> builder() {
        return new Builder<>();
    }

    public static class Builder<I, O> {
        private Mapper<RoutingContext, I> inputMapper;
        private Mapper<O, Content> outputMapper;
        private Controller<I, O> controller;
        private SuccessHandler successHandler;
        private FailureHandler failureHandler;

        public Builder() {}

        public Builder<I, O> withInputMapper(Mapper<RoutingContext, I> requestMapper) {
            this.inputMapper = requestMapper;
            return this;
        }

        public Builder<I, O> withOutputMapper(Mapper<O, Content> responseMapper) {
            this.outputMapper = responseMapper;
            return this;
        }

        public Builder<I, O> withController(Controller<I, O> controller) {
            this.controller = controller;
            return this;
        }

        public Builder<I, O> onSuccess(SuccessHandler successHandler) {
            this.successHandler = successHandler;
            return this;
        }

        public Builder<I, O> onFailure(FailureHandler failureHandler) {
            this.failureHandler = failureHandler;
            return this;
        }

        public RESTContentHandler<I, O> build() {
            return new RESTContentHandler<>(inputMapper, outputMapper, controller, successHandler, failureHandler);
        }
    }
}
