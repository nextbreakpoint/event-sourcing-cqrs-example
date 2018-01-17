package com.nextbreakpoint.shop.designs.delete;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DeleteDesignsRequestMapper implements RequestMapper<DeleteDesignsRequest> {
    @Override
    public DeleteDesignsRequest apply(RoutingContext context) {
        return new DeleteDesignsRequest();
    }
}
