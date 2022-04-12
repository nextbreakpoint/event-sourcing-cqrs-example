package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.ContentType;
import com.nextbreakpoint.blueprint.common.core.Headers;
import com.nextbreakpoint.blueprint.common.core.Image;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.function.BiConsumer;

public class PNGConsumer implements BiConsumer<RoutingContext, Image> {
    private int statusCode;

    public PNGConsumer(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void accept(RoutingContext context, Image image) {
        context.response()
                .putHeader(Headers.CONTENT_TYPE, ContentType.IMAGE_PNG)
                .putHeader(Headers.CACHE_CONTROL, "private,max-age=604800,s-maxage=604800,stale-if-error=10")
                .putHeader(Headers.ETAG, image.getChecksum())
                .setStatusCode(statusCode)
                .rxSend(Buffer.buffer(image.getData()))
                .subscribe();
    }
}
