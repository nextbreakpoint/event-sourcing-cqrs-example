package com.nextbreakpoint.shop.designs.load;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class LoadDesignRequestMapper implements RequestMapper<LoadDesignRequest> {
    @Override
    public LoadDesignRequest apply(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        return new LoadDesignRequest(UUID.fromString(uuid));
    }
}
