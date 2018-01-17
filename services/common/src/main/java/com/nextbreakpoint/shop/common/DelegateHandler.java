package com.nextbreakpoint.shop.common;

import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;

import static rx.Single.fromCallable;

public class DelegateHandler<T, R> implements Handler<RoutingContext> {
    private final RequestMapper<T> requestMapper;
    private final Controller<T, R> controller;
    private final ResponseMapper<R> responseMapper;
    private final SuccessHandler successHandler;
    private final FailureHandler failureHandler;

    private DelegateHandler(RequestMapper<T> requestMapper, Controller<T, R> controller, ResponseMapper<R> responseMapper, SuccessHandler successHandler, FailureHandler failureHandler) {
        this.requestMapper = Objects.requireNonNull(requestMapper);
        this.controller = Objects.requireNonNull(controller);
        this.responseMapper = Objects.requireNonNull(responseMapper);
        this.successHandler = Objects.requireNonNull(successHandler);
        this.failureHandler = Objects.requireNonNull(failureHandler);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        fromCallable(() -> requestMapper.apply(routingContext))
                .flatMap(request -> controller.apply(request))
                .map(response -> responseMapper.apply(response))
                .subscribe(result -> successHandler.apply(routingContext, result), err -> failureHandler.apply(routingContext, err));
    }

    public static <T, R> DelegateHandlerBuilder<T, R> builder() {
        return new DelegateHandlerBuilder();
    }

    public static class DelegateHandlerBuilder<T, R> {
        private RequestMapper<T> requestMapper;
        private Controller<T, R> controller;
        private ResponseMapper<R> responseMapper;
        private SuccessHandler successHandler;
        private FailureHandler failureHandler;

        public DelegateHandlerBuilder() {}

        public DelegateHandlerBuilder<T, R> with(RequestMapper<T> requestMapper) {
            this.requestMapper = requestMapper;
            return this;
        }

        public DelegateHandlerBuilder<T, R> with(Controller<T, R> controller) {
            this.controller = controller;
            return this;
        }

        public DelegateHandlerBuilder<T, R> with(ResponseMapper<R> responseMapper) {
            this.responseMapper = responseMapper;
            return this;
        }

        public DelegateHandlerBuilder<T, R> with(SuccessHandler successHandler) {
            this.successHandler = successHandler;
            return this;
        }

        public DelegateHandlerBuilder<T, R> with(FailureHandler failureHandler) {
            this.failureHandler = failureHandler;
            return this;
        }

        public DelegateHandler<T, R> build() {
            return new DelegateHandler<>(requestMapper, controller, responseMapper, successHandler, failureHandler);
        }
    }
}
