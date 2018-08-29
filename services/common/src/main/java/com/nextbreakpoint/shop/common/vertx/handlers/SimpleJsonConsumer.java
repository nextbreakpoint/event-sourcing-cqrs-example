package com.nextbreakpoint.shop.common.handlers;

import io.vertx.rxjava.ext.web.RoutingContext;
import kafka.utils.Json;

import java.util.function.BiConsumer;

import static com.nextbreakpoint.shop.common.model.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.model.Headers.CONTENT_TYPE;

public class SimpleJsonConsumer implements BiConsumer<RoutingContext, String> {
    private int statusCode;

    public SimpleJsonConsumer(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void accept(RoutingContext context, String json) {
        context.response()
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(statusCode)
                .end(Json.encode(json));
    }
}
