package com.nextbreakpoint.shop.common.vertx;

import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.List;

public class AccessHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(AccessHandler.class.getName());

    private final JWTAuth jwtProvider;
    private final List<String> authorities;
    private final Handler<RoutingContext> onAccessDenied;
    private final Handler<RoutingContext> onAccessGranted;

    public AccessHandler(JWTAuth jwtProvider, Handler<RoutingContext> onAccessGranted, Handler<RoutingContext> onAccessDenied, List<String> authorities) {
        this.jwtProvider = jwtProvider;
        this.authorities = authorities;
        this.onAccessDenied = onAccessDenied;
        this.onAccessGranted = onAccessGranted;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        Authentication.isUserAuthorized(jwtProvider, routingContext, authorities)
                .doOnSuccess(user -> logger.debug("User is authenticated and authorized"))
                .subscribe(user -> onUserAuthorized(routingContext, user), err -> onAuthenticationError(routingContext, err));
    }

    private void onAuthenticationError(RoutingContext routingContext, Throwable err) {
        logger.info("Authentication error", err);
        onAccessDenied.handle(routingContext);
    }

    private void onUserAuthorized(RoutingContext routingContext, User user) {
        routingContext.setUser(user);
        onAccessGranted.handle(routingContext);
    }
}
