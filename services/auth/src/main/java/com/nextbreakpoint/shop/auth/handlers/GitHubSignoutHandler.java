package com.nextbreakpoint.shop.auth.handlers;

import com.nextbreakpoint.shop.common.model.Failure;
import com.nextbreakpoint.shop.common.vertx.Authentication;
import com.nextbreakpoint.shop.common.vertx.WebClientFactory;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.WebClient;

import java.net.MalformedURLException;

public class GitHubSignoutHandler implements Handler<RoutingContext> {
    private final WebClient githubClient;
    private final String cookieDomain;
    private final String webUrl;

    public GitHubSignoutHandler(Vertx vertx, JsonObject config, Router router) throws MalformedURLException {
        cookieDomain = config.getString("cookie_domain");
        webUrl = config.getString("client_web_url");
        githubClient = WebClientFactory.create(vertx, config.getString("github_url"));
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
        return webUrl + routingContext.request().path().substring("/a/auth/signout".length());
    }

    protected void sendRedirectResponse(RoutingContext routingContext, String redirectTo, Cookie cookie) {
        routingContext.response()
                .putHeader("Set-Cookie", cookie.encode())
                .putHeader("Location", redirectTo)
                .setStatusCode(303)
                .end();
    }
}
