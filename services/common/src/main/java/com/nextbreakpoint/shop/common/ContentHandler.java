package com.nextbreakpoint.shop.common;

import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.function.BiConsumer;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;

public class ContentHandler implements BiConsumer<RoutingContext, Content> {
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
    public void accept(RoutingContext context, Content content) {
        final HttpServerResponse response = context.response()
                .putHeader(CONTENT_TYPE, APPLICATION_JSON);

        content.getMetadata().stream()
                .forEach(header -> response.putHeader("X-" + header.getName(), header.getValue()));

        if (content.getJson().isPresent()) {
            response.setStatusCode(statusCode).end(content.getJson().get());
        } else {
            response.setStatusCode(emptyStatusCode).end();
        }
    }
}
