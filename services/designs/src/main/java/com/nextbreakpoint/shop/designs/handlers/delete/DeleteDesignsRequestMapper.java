package com.nextbreakpoint.shop.designs.handlers.delete;

import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DeleteDesignsRequestMapper implements Mapper<RoutingContext, DeleteDesignsRequest> {
    @Override
    public DeleteDesignsRequest transform(RoutingContext context) {
        return new DeleteDesignsRequest();
    }
}
