package com.nextbreakpoint.blueprint.designs.controllers.load;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.LoadDesignRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class LoadDesignRequestMapper implements Mapper<RoutingContext, LoadDesignRequest> {
    @Override
    public LoadDesignRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        return new LoadDesignRequest(UUID.fromString(uuid));
    }
}
