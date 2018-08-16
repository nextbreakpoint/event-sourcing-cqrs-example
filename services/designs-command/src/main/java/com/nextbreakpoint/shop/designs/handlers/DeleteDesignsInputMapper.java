package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsEvent;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DeleteDesignsInputMapper implements Mapper<RoutingContext, DeleteDesignsEvent> {
    @Override
    public DeleteDesignsEvent transform(RoutingContext context) {
        return new DeleteDesignsEvent();
    }
}
