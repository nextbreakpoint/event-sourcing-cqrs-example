package com.nextbreakpoint.shop.designs.get;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class GetStatusRequestMapper implements RequestMapper<GetStatusRequest> {
    @Override
    public GetStatusRequest apply(RoutingContext context) {
        return new GetStatusRequest();
    }
}
