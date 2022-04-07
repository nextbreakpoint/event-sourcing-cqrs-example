package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteDesignRequestMapper implements Mapper<RoutingContext, DeleteDesignRequest> {
    @Override
    public DeleteDesignRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("designId");

        if (uuid == null) {
            throw new IllegalStateException("the required parameter designId is missing");
        }

        final JsonObject principal = context.user().principal();

        final UUID owner = UUID.fromString(principal.getString("user"));

        return DeleteDesignRequest.builder()
                .withOwner(owner)
                .withUuid(UUID.fromString(uuid))
                .withChange(UUID.randomUUID())
                .build();
    }
}
