package com.nextbreakpoint.blueprint.designs.controllers.list;

import com.nextbreakpoint.blueprint.designs.model.ListDesignsRequest;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ListDesignsInputMapper implements Mapper<RoutingContext, ListDesignsRequest> {
    @Override
    public ListDesignsRequest transform(RoutingContext context) {
        return new ListDesignsRequest();
    }
}
