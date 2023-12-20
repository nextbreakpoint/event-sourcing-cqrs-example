package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.authentication.common.RoutingContextAdapter;
import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.http.Cookie;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

@Log4j2
public class GitHubSignOutHandler implements Handler<RoutingContextAdapter> {
    private final String cookieDomain;

    public GitHubSignOutHandler(String cookieDomain) {
        this.cookieDomain = Objects.requireNonNull(cookieDomain);
    }

    @Override
    public void handle(RoutingContextAdapter routingContext) {
        Single.just(newScope(routingContext))
                .map(scope -> scope.toBuilder().withCookie(createUnauthenticatedCookie()).build())
                .flatMap(this::sendRedirectResponse)
                .subscribe(scope -> {}, routingContext::handleException);
    }

    private GitHubSignOutScope newScope(RoutingContextAdapter routingContext) {
        return GitHubSignOutScope.builder()
                .withRoutingContext(routingContext)
                .withRedirectTo(routingContext.getSignOutRedirectUrl())
                .build();
    }

    private Cookie createUnauthenticatedCookie() {
        return Authentication.createCookie("", cookieDomain);
    }

    private Single<Void> sendRedirectResponse(GitHubSignOutScope scope) {
        return scope.getRoutingContext().sendRedirectResponse(scope.getRedirectTo(), scope.getCookie());
    }
}
