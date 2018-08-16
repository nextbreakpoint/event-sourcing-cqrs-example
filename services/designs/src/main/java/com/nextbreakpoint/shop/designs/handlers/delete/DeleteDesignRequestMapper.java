package com.nextbreakpoint.shop.designs.handlers.delete;

import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteDesignRequestMapper implements Mapper<RoutingContext, DeleteDesignRequest> {
    @Override
    public DeleteDesignRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        return new DeleteDesignRequest(UUID.fromString(uuid));
    }
}
