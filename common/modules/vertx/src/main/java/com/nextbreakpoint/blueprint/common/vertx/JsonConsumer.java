package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.ContentType;
import com.nextbreakpoint.blueprint.common.core.Headers;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.function.BiConsumer;

public class JsonConsumer implements BiConsumer<RoutingContext, String> {
    private int statusCode;

    public JsonConsumer(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void accept(RoutingContext context, String json) {
        context.response()
                .putHeader(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .setStatusCode(statusCode)
                .end(json);
    }
}
