package com.nextbreakpoint.shop.designs.controllers.list;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ListDesignsRequestMapper implements Mapper<RoutingContext, ListDesignsRequest> {
    @Override
    public ListDesignsRequest transform(RoutingContext context) {
        return new ListDesignsRequest();
    }
}
