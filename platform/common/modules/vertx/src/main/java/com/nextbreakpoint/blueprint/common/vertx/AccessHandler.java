package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.Handler;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Objects;

@Log4j2
public class AccessHandler implements Handler<RoutingContext> {
    private final JWTAuth jwtProvider;
    private final List<String> authorities;
    private final Handler<RoutingContext> onAccessDenied;
    private final Handler<RoutingContext> onAccessGranted;

    public AccessHandler(JWTAuth jwtProvider, Handler<RoutingContext> onAccessGranted, Handler<RoutingContext> onAccessDenied, List<String> authorities) {
        this.jwtProvider = Objects.requireNonNull(jwtProvider);
        this.authorities = Objects.requireNonNull(authorities);
        this.onAccessDenied = Objects.requireNonNull(onAccessDenied);
        this.onAccessGranted = Objects.requireNonNull(onAccessGranted);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        Authentication.isUserAuthorized(jwtProvider, routingContext, authorities)
                .doOnSuccess(user -> log.debug("User is authenticated and authorized"))
                .subscribe(user -> onUserAuthorized(routingContext, user), err -> onAuthenticationError(routingContext, err));
    }

    private void onAuthenticationError(RoutingContext routingContext, Throwable err) {
        if (log.isTraceEnabled()) {
            log.trace("User is not authorized", err);
        } else {
            log.debug("User is not authorized");
        }
        onAccessDenied.handle(routingContext);
    }

    private void onUserAuthorized(RoutingContext routingContext, User user) {
        routingContext.setUser(user);
        onAccessGranted.handle(routingContext);
    }
}
