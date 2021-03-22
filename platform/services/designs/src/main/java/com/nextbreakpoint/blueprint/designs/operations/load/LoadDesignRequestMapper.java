package com.nextbreakpoint.blueprint.designs.operations.load;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class LoadDesignRequestMapper implements Mapper<RoutingContext, LoadDesignRequest> {
    @Override
    public LoadDesignRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("designId");

        return new LoadDesignRequest(UUID.fromString(uuid));
    }
}
