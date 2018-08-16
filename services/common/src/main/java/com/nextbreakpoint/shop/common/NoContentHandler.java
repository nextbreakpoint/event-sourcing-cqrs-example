package com.nextbreakpoint.shop.common;

import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

public class NoContentHandler implements SuccessHandler {
    private int statusCode;

    public NoContentHandler(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void apply(RoutingContext context, Content result) {
        final HttpServerResponse response = context.response();

        result.getMetadata().stream()
                .forEach(header -> response.putHeader(header.getName(), header.getValue()));

        response.setStatusCode(statusCode).end();
    }
}
