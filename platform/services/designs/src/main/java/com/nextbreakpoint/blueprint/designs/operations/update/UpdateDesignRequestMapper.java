package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class UpdateDesignRequestMapper implements Mapper<RoutingContext, UpdateDesignRequest> {
    @Override
    public UpdateDesignRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("designId");

        final JsonObject bodyAsJson = context.getBodyAsJson();

        final String json = new JsonObject()
                .put("manifest", bodyAsJson.getString("manifest"))
                .put("metadata", bodyAsJson.getString("metadata"))
                .put("script", bodyAsJson.getString("script"))
                .encode();

        return new UpdateDesignRequest(UUID.fromString(uuid), json);
    }
}
