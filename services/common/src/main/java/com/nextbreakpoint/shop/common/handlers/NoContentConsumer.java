package com.nextbreakpoint.shop.common.handlers;

import com.nextbreakpoint.shop.common.model.Content;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.function.BiConsumer;

public class NoContentConsumer implements BiConsumer<RoutingContext, Content> {
    private int statusCode;

    public NoContentConsumer(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void accept(RoutingContext context, Content result) {
        final HttpServerResponse response = context.response();

        result.getMetadata().stream()
                .forEach(header -> response.putHeader(header.getName(), header.getValue()));

        response.setStatusCode(statusCode).end();
    }
}
