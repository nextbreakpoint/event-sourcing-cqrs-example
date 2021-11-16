package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.ContentType;
import com.nextbreakpoint.blueprint.common.core.Headers;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.function.BiConsumer;

public class PNGConsumer implements BiConsumer<RoutingContext, byte[]> {
    private int statusCode;

    public PNGConsumer(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void accept(RoutingContext context, byte[] data) {
        context.response()
                .putHeader(Headers.CONTENT_TYPE, ContentType.IMAGE_PNG)
                .setStatusCode(statusCode)
                .end(Buffer.buffer(data));
    }
}
