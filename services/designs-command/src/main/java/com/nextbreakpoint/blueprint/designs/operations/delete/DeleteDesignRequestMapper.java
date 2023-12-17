package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteDesignRequestMapper implements Mapper<RoutingContext, DeleteDesignRequest> {
    @Override
    public DeleteDesignRequest transform(RoutingContext context) {
        final String uuidParam = context.request().getParam("designId");

        if (uuidParam == null) {
            throw new IllegalStateException("the required parameter designId is missing");
        }

        if (context.user() == null) {
            throw new IllegalStateException("the user is not authenticated");
        }

        final JsonObject principal = context.user().principal();

        try {
            final UUID owner = UUID.fromString(principal.getString("user"));

            final UUID uuid = UUID.fromString(uuidParam);

            return DeleteDesignRequest.builder()
                    .withOwner(owner)
                    .withUuid(uuid)
                    .withChange(UUID.randomUUID())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("invalid request: " + e.getMessage());
        }
    }
}
