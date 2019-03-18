package com.nextbreakpoint.shop.designs.controllers.load;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class LoadDesignInputMapper implements Mapper<RoutingContext, LoadDesignRequest> {
    @Override
    public LoadDesignRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        if (uuid == null) {
            throw new IllegalStateException("parameter uuid (param0) missing from routing context");
        }

        return new LoadDesignRequest(UUID.fromString(uuid));
    }
}
