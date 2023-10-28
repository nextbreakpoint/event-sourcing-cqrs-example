package com.nextbreakpoint.blueprint.authentication.handlers;

import io.vertx.rxjava.core.http.Cookie;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with", toBuilder = true)
public class GitHubSignOutScope {
    private RoutingContext routingContext;
    private User user;
    private String redirectTo;
    private Cookie cookie;
}
