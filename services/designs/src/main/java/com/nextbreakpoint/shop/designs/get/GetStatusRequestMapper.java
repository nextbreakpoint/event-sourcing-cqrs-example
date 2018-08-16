package com.nextbreakpoint.shop.designs.get;

import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class GetStatusRequestMapper implements Mapper<RoutingContext, GetStatusRequest> {
    @Override
    public GetStatusRequest transform(RoutingContext context) {
        return new GetStatusRequest();
    }
}
