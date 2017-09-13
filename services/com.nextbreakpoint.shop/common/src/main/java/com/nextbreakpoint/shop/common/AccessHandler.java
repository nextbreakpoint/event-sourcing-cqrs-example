package com.nextbreakpoint.shop.common;

import io.vertx.core.Handler;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccessHandler implements Handler<RoutingContext> {
    private static final Logger logger = Logger.getLogger(AccessHandler.class.getName());

    private final JWTAuth jwtProvider;
    private final List<String> authorities;
    private final Consumer<RoutingContext> onAccessDenied;

    public AccessHandler(JWTAuth jwtProvider, Consumer<RoutingContext> onAccessDenied, List<String> authorities) {
        this.jwtProvider = jwtProvider;
        this.authorities = authorities;
        this.onAccessDenied = onAccessDenied;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processAccess(routingContext, onAccessDenied, authorities);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processAccess(RoutingContext routingContext, Consumer<RoutingContext> onAccessDenied, List<String> roles) {
        Authentication.isUserAuthorized(jwtProvider, routingContext, roles)
                .doOnError(err -> logger.log(Level.FINE, err.getMessage(), err))
                .subscribe(user -> processUser(routingContext, user), err -> onAccessDenied.accept(routingContext));
    }

    private void processUser(RoutingContext routingContext, User user) {
        routingContext.setUser(user);
        routingContext.next();
    }
}
