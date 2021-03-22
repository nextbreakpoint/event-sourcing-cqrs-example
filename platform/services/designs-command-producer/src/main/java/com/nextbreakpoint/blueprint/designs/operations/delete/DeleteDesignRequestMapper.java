package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteDesignRequestMapper implements Mapper<RoutingContext, DeleteDesignRequest> {
    @Override
    public DeleteDesignRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("designId");

        if (uuid == null) {
            throw new IllegalStateException("parameter uuid (designId) missing from routing context");
        }

        return new DeleteDesignRequest(UUID.fromString(uuid));
    }
}
