package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.Cookie;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.net.MalformedURLException;

public class GitHubSignOutHandler implements Handler<RoutingContext> {
    private final String cookieDomain;
    private final String webUrl;

    public GitHubSignOutHandler(Vertx vertx, JsonObject config, Router router) throws MalformedURLException {
        cookieDomain = config.getString("cookie_domain");
        webUrl = config.getString("client_web_url");
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            sendRedirectResponse(routingContext, getRedirectTo(routingContext), Authentication.createCookie("", cookieDomain));
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    protected String getRedirectTo(RoutingContext routingContext) {
        return webUrl + routingContext.request().path().substring("/auth/signout".length());
    }

    protected void sendRedirectResponse(RoutingContext routingContext, String redirectTo, Cookie cookie) {
        routingContext.response()
                .putHeader("Set-Cookie", cookie.encode())
                .putHeader("Location", redirectTo)
                .setStatusCode(303)
                .end();
    }
}
