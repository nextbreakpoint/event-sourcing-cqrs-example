package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignEvent;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteDesignInputMapper implements Mapper<RoutingContext, DeleteDesignEvent> {
    @Override
    public DeleteDesignEvent transform(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        if (uuid == null) {
            throw new IllegalStateException("parameter uuid (param0) missing from routing context");
        }

        return new DeleteDesignEvent(UUID.fromString(uuid), System.currentTimeMillis());
    }
}
