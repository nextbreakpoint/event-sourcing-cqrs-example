package com.nextbreakpoint.shop.web;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.WebClient;

import java.util.List;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;

public class DesignWatchHandler extends WatchHandler {
    protected DesignWatchHandler(Vertx vertx, JWTAuth jwtProvider, WebClient client, JsonObject config) {
        super(vertx, jwtProvider, client, config);
    }

    @Override
    protected void pollBucket(List<String> bucket) {
        final JsonArray uuids = bucket.stream().collect(JsonArray::new, JsonArray::add, (a, b) -> {});

        getClient().get("/designs/state")
                .putHeader(AUTHORIZATION, makeAccessToken())
                .putHeader(ACCEPT, APPLICATION_JSON)
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .rxSendBuffer(Buffer.buffer(uuids.encode()))
                .subscribe(response -> handleState(response), e -> handleFailure(e));
    }

    @Override
    protected String getWatchKey(RoutingContext routingContext) {
        return routingContext.pathParam("param1");
    }

    @Override
    protected Long getOffset(RoutingContext routingContext) {
        return Long.parseLong(routingContext.pathParam("param0"));
    }

    public static Handler<RoutingContext> create(Vertx vertx, JWTAuth jwtProvider, WebClient client, JsonObject config) {
        return new DesignWatchHandler(vertx, jwtProvider, client, config);
    }
}