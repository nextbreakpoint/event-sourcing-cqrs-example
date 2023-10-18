package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.http.Cookie;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.composer.ComposerException;
import rx.Single;

import java.util.Objects;

@Log4j2
public class GitHubSignOutHandler implements Handler<RoutingContext> {
    private final String cookieDomain;
    private final String webUrl;

    public GitHubSignOutHandler(String cookieDomain, String webUrl) {
        this.cookieDomain = Objects.requireNonNull(cookieDomain);
        this.webUrl = Objects.requireNonNull(webUrl);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        Single.just(newScope(routingContext))
                .flatMap(scope -> getCookie(scope).map(cookie -> scope.toBuilder().withCookie(cookie).build()))
                .flatMap(this::sendRedirectResponse)
                .subscribe(scope -> {}, throwable -> handleException(routingContext, throwable));
    }

    private GitHubSignOutScope newScope(RoutingContext routingContext) {
        return GitHubSignOutScope.builder()
                .withRoutingContext(routingContext)
                .withUser(routingContext.user())
                .withRedirectTo(getRedirectTo(routingContext))
                .build();
    }

    private Single<Cookie> getCookie(GitHubSignOutScope scope) {
        return Single.fromCallable(() -> Authentication.createCookie("", cookieDomain));
    }

    private Single<Void> sendRedirectResponse(GitHubSignOutScope scope) {
        return scope.getRoutingContext().response()
                .putHeader("Set-Cookie", scope.getCookie().encode())
                .putHeader("Location", scope.getRedirectTo())
                .setStatusCode(303)
                .rxSend();
    }

    private String getRedirectTo(RoutingContext routingContext) {
        return webUrl + routingContext.request().path().substring("/v1/auth/signout".length());
    }

    private void handleException(RoutingContext routingContext, Throwable throwable) {
        if (throwable instanceof Failure) {
            routingContext.fail(throwable);
        } else if (throwable instanceof ComposerException) {
            log.error("Cannot process request", throwable);
            routingContext.fail(Failure.requestFailed(throwable));
        }
    }
}
