package com.nextbreakpoint.shop.web.handlers;

import com.nextbreakpoint.shop.common.model.Failure;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class UUIDInjectHandler implements Handler<RoutingContext> {
    private UUIDInjectHandler() {}

    public void handle(RoutingContext routingContext) {
        try {
            final String uuid = routingContext.pathParam("param0");

            routingContext.put("uuid", uuid);

            routingContext.next();
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    public static UUIDInjectHandler create() {
        return new UUIDInjectHandler();
    }
}