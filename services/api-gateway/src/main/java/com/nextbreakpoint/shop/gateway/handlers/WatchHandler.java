package com.nextbreakpoint.shop.gateway.handlers;

import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;

import static com.nextbreakpoint.shop.common.model.Headers.LOCATION;

public class WatchHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(WatchHandler.class.getName());

    private final String watchURL;

    public WatchHandler(String watchURL) {
        this.watchURL = Objects.requireNonNull(watchURL);
    }

    @Override
    public void handle(RoutingContext context) {
        // we should select the server according to user and resource
        // final String user = context.user().principal().getString("user");

        final String resource = context.request().uri().substring("/watch/designs/".length());

        logger.info("Redirect watch request to resource " + watchURL + "/" + resource);

        context.response().putHeader(LOCATION, watchURL + "/" + resource).setStatusCode(200).end();
    }
}
