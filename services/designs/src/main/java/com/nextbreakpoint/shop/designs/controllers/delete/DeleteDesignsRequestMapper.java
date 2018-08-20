package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DeleteDesignsRequestMapper implements Mapper<RoutingContext, DeleteDesignsRequest> {
    @Override
    public DeleteDesignsRequest transform(RoutingContext context) {
        return new DeleteDesignsRequest();
    }
}
