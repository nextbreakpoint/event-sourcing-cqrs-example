package com.nextbreakpoint.blueprint.designs.handlers;

import io.vertx.core.Handler;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.Record;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.nextbreakpoint.blueprint.common.core.Headers.LOCATION;

public class WatchHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(WatchHandler.class.getName());

    private final ServiceDiscovery serviceDiscovery;

    public WatchHandler(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = Objects.requireNonNull(serviceDiscovery);
    }

    private boolean isDesignsSSE(Record record) {
        return record.getName().equals("designs-sse");
    }

    @Override
    public void handle(RoutingContext context) {
        final List<Record> records = serviceDiscovery
                .rxGetRecords(this::isDesignsSSE)
                .timeout(5, TimeUnit.SECONDS)
                .toBlocking()
                .value();

        if (records.size() > 0) {
            // use a random uniform distribution for now
            int index = (int) Math.round(Math.random() * (records.size() - 1));

            // we could select the server according to usage or user/resource
            // final String user = context.user().principal().getString("user");

            final Record selectedRecord = records.get(index);

            final String host = selectedRecord.getMetadata().getString("ServiceAddress");
            final Integer port = selectedRecord.getMetadata().getInteger("ServicePort");

            final String uri = context.request().uri().replaceFirst("/watch", "/sse");

            final String resource = "https://" + host + ":" + port + uri;

            logger.info("Redirect watch request to resource " + resource);

            context.response().putHeader(LOCATION, resource).setStatusCode(200).end();
        } else {
            logger.warn("No records found for service designs-sse");

            context.response().setStatusCode(404).end();
        }
    }
}
