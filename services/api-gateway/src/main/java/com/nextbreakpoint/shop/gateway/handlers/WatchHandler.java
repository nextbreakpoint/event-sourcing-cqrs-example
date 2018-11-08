package com.nextbreakpoint.shop.gateway.handlers;

import com.nextbreakpoint.shop.common.vertx.ResponseHelper;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;

public class WatchHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(WatchHandler.class.getName());

    private final String watchURL;

    public WatchHandler(String watchURL) {
        this.watchURL = Objects.requireNonNull(watchURL);
    }

    @Override
    public void handle(RoutingContext context) {
//        final String user = context.user().principal().getString("user");
//
//        if (user == null || !context.request().uri().startsWith("/watch/")) {
//            context.fail(500);
//        }

        // we should select the server according to user and resource

        final String resource = context.request().uri().substring("/watch/designs/".length());

        logger.info("Redirect watch request to resource " + watchURL + "/" + resource);

        ResponseHelper.redirectToURL(context, () -> watchURL + "/" + resource);
    }
}
