package com.nextbreakpoint.blueprint.designs.controllers.delete;

import com.datastax.driver.core.utils.UUIDs;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.command.DeleteDesign;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteDesignInputMapper implements Mapper<RoutingContext, DeleteDesign> {
    @Override
    public DeleteDesign transform(RoutingContext context) {
        final String uuid = context.request().getParam("designId");

        if (uuid == null) {
            throw new IllegalStateException("parameter uuid (designId) missing from routing context");
        }

        return new DeleteDesign(UUID.fromString(uuid), UUIDs.timeBased());
    }
}
