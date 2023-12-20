package com.nextbreakpoint.blueprint.authentication.common;

import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.rxjava.core.http.Cookie;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.composer.ComposerException;
import rx.Single;

import java.util.Objects;

import static io.vertx.core.http.HttpHeaders.CACHE_CONTROL;
import static io.vertx.core.http.HttpHeaders.EXPIRES;

@Log4j2
public class RoutingContextAdapter {
    private final RoutingContext routingContext;
    private final String webUrl;

    public RoutingContextAdapter(RoutingContext routingContext, String webUrl) {
        this.routingContext = Objects.requireNonNull(routingContext);
        this.webUrl = Objects.requireNonNull(webUrl);
    }

    public boolean isUserAuthenticated() {
        return routingContext.user() != null;
    }

    public String getRequestUri() {
        return routingContext.request().uri();
    }

    public String getAccessToken() {
        return routingContext.user().principal().getString("access_token");
    }

    public String getSignInRedirectUrl() {
        return webUrl + routingContext.request().path().substring("/v1/auth/signin".length());
    }

    public String getSignOutRedirectUrl() {
        return webUrl + routingContext.request().path().substring("/v1/auth/signout".length());
    }

    public String getRequestParam(String name) {
        return routingContext.request().getParam(name);
    }

    public Single<Void> sendRedirectResponse(String location) {
        return routingContext.response()
                .putHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .putHeader("Pragma", "no-cache")
                .putHeader(EXPIRES, "0")
                .putHeader("Location", location)
                .setStatusCode(302)
                .rxEnd();
    }

    public Single<Void> sendRedirectResponse(String location, Cookie cookie) {
        return routingContext.response()
                .putHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .putHeader("Pragma", "no-cache")
                .putHeader(EXPIRES, "0")
                .putHeader("Location", location)
                .putHeader("Set-Cookie", cookie.encode())
                .setStatusCode(303)
                .rxEnd();
    }

    public void reroute(String location) {
        routingContext.reroute(location);
    }

    public void fail(Failure failure) {
        routingContext.fail(failure);
    }

    public void fail(int code, Throwable cause) {
        routingContext.fail(code, cause);
    }

    public void setUser(User user) {
        routingContext.setUser(user);
    }

    public void handleException(Throwable throwable) {
        if (throwable instanceof Failure) {
            routingContext.fail(throwable);
        } else if (throwable instanceof ComposerException) {
            log.error("Cannot process request", throwable);
            routingContext.fail(Failure.requestFailed(throwable.getCause()));
        } else {
            routingContext.fail(Failure.requestFailed(throwable));
        }
    }
}
