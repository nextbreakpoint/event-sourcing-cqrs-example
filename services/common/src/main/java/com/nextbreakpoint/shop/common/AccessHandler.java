package com.nextbreakpoint.shop.common;

import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.List;
import java.util.function.Consumer;

public class AccessHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(AccessHandler.class.getName());

    private final JWTAuth jwtProvider;
    private final List<String> authorities;
    private final Handler<RoutingContext> onAccessDenied;
    private final Handler<RoutingContext> onAccessGranted;

    public AccessHandler(JWTAuth jwtProvider, Consumer<RoutingContext> onAccessDenied, List<String> authorities) {
        this.jwtProvider = jwtProvider;
        this.authorities = authorities;
        this.onAccessDenied = routingContext -> onAccessDenied.accept(routingContext);
        this.onAccessGranted = routingContext -> routingContext.next();
    }

    public AccessHandler(JWTAuth jwtProvider, Handler<RoutingContext> onAccessGranted, Handler<RoutingContext> onAccessDenied, List<String> authorities) {
        this.jwtProvider = jwtProvider;
        this.authorities = authorities;
        this.onAccessDenied = onAccessDenied;
        this.onAccessGranted = onAccessGranted;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        Authentication.isUserAuthorized(jwtProvider, routingContext, authorities)
                .doOnError(err -> logger.debug("Authorization failed", err))
                .subscribe(user -> processUser(routingContext, user), err -> processError(routingContext));
    }

    private void processError(RoutingContext routingContext) {
        onAccessDenied.handle(routingContext);
    }

    private void processUser(RoutingContext routingContext, User user) {
        routingContext.setUser(user);
        onAccessGranted.handle(routingContext);
    }
}
