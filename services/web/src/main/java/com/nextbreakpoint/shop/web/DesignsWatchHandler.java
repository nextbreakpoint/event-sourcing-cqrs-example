package com.nextbreakpoint.shop.web;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.WebClient;

import java.util.List;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.Headers.AUTHORIZATION;

public class DesignsWatchHandler extends WatchHandler {
    protected DesignsWatchHandler(Vertx vertx, JWTAuth jwtProvider, WebClient client, JsonObject config) {
        super(vertx, jwtProvider, client, config);
    }

    public static Handler<RoutingContext> create(Vertx vertx, JWTAuth jwtProvider, WebClient client, JsonObject config) {
        return new DesignsWatchHandler(vertx, jwtProvider, client, config);
    }

    @Override
    protected void pollBucket(List<String> bucket) {
        getClient().get("/api/designs/status")
                .putHeader(AUTHORIZATION, makeAccessToken())
                .putHeader(ACCEPT, APPLICATION_JSON)
                .rxSend()
                .subscribe(response -> handleState(response), e -> handleFailure(e));
    }

    @Override
    protected String getWatchKey(RoutingContext routingContext) {
        return "designs";
    }

    @Override
    protected Long getOffset(RoutingContext routingContext) {
        return Long.parseLong(routingContext.pathParam("param0"));
    }
}