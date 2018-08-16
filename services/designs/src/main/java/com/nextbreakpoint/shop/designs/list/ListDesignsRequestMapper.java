package com.nextbreakpoint.shop.designs.list;

import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ListDesignsRequestMapper implements Mapper<RoutingContext, ListDesignsRequest> {
    @Override
    public ListDesignsRequest transform(RoutingContext context) {
        return new ListDesignsRequest();
    }
}
