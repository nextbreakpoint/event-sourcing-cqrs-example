package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.DeleteDesignEvent;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteDesignInputMapper implements Mapper<RoutingContext, DeleteDesignEvent> {
    @Override
    public DeleteDesignEvent transform(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        if (uuid == null) {
            throw new IllegalStateException("parameter uuid (param0) missing from routing context");
        }

        return new DeleteDesignEvent(UUID.fromString(uuid));
    }
}
