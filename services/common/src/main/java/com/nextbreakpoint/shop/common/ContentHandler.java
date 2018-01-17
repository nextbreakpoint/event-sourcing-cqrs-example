package com.nextbreakpoint.shop.common;

import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;

public class ContentHandler implements SuccessHandler {
    private int statusCode;
    private int emptyStatusCode;

    public ContentHandler(int statusCode) {
        this.statusCode = statusCode;
        this.emptyStatusCode = statusCode;
    }

    public ContentHandler(int statusCode, int emptyStatusCode) {
        this.statusCode = statusCode;
        this.emptyStatusCode = emptyStatusCode;
    }

    @Override
    public void apply(RoutingContext context, Result result) {
        final HttpServerResponse response = context.response()
                .putHeader(CONTENT_TYPE, APPLICATION_JSON);

        result.getHeaders().stream()
                .forEach(header -> response.putHeader(header.getName(), header.getValue()));

        if (result.getJson().isPresent()) {
            response.setStatusCode(statusCode).end(result.getJson().get());
        } else {
            response.setStatusCode(emptyStatusCode).end();
        }
    }
}
