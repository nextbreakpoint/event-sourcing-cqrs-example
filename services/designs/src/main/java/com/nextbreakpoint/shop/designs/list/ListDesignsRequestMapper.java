package com.nextbreakpoint.shop.designs.list;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ListDesignsRequestMapper implements RequestMapper<ListDesignsRequest> {
    @Override
    public ListDesignsRequest apply(RoutingContext context) {
        return new ListDesignsRequest();
    }
}
