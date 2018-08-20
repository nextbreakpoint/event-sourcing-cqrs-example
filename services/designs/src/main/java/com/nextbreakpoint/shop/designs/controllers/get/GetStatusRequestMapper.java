package com.nextbreakpoint.shop.designs.controllers.get;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.GetStatusRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

public class GetStatusRequestMapper implements Mapper<RoutingContext, GetStatusRequest> {
    @Override
    public GetStatusRequest transform(RoutingContext context) {
        return new GetStatusRequest();
    }
}
