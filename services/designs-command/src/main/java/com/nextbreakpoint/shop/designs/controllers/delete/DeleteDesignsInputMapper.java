package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignsEvent;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DeleteDesignsInputMapper implements Mapper<RoutingContext, DeleteDesignsEvent> {
    @Override
    public DeleteDesignsEvent transform(RoutingContext context) {
        return new DeleteDesignsEvent(System.currentTimeMillis());
    }
}
