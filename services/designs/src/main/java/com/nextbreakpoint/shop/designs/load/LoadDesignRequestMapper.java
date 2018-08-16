package com.nextbreakpoint.shop.designs.load;

import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class LoadDesignRequestMapper implements Mapper<RoutingContext, LoadDesignRequest> {
    @Override
    public LoadDesignRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        return new LoadDesignRequest(UUID.fromString(uuid));
    }
}
