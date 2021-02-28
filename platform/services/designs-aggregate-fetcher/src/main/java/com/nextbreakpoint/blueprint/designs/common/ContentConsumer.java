package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Content;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.function.BiConsumer;

import static com.nextbreakpoint.blueprint.common.core.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;

public class ContentConsumer implements BiConsumer<RoutingContext, Content> {
    private int statusCode;
    private int emptyStatusCode;

    public ContentConsumer(int statusCode) {
        this.statusCode = statusCode;
        this.emptyStatusCode = statusCode;
    }

    public ContentConsumer(int statusCode, int emptyStatusCode) {
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
