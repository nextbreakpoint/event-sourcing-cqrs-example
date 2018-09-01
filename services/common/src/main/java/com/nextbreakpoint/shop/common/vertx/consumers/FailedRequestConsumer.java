package com.nextbreakpoint.shop.common.vertx.consumers;

import com.nextbreakpoint.shop.common.model.Failure;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.function.BiConsumer;

public class FailedRequestConsumer implements BiConsumer<RoutingContext, Throwable> {
    @Override
    public void accept(RoutingContext context, Throwable error) {
        context.fail(Failure.requestFailed(error));
    }
}
