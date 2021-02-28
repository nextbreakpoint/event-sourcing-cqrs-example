package com.nextbreakpoint.blueprint.designs.controllers.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.UpdateDesignRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class UpdateDesignRequestMapper implements Mapper<RoutingContext, UpdateDesignRequest> {
    @Override
    public UpdateDesignRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        final JsonObject bodyAsJson = context.getBodyAsJson();

        final String json = new JsonObject()
                .put("manifest", bodyAsJson.getString("manifest"))
                .put("metadata", bodyAsJson.getString("metadata"))
                .put("script", bodyAsJson.getString("script"))
                .encode();

        return new UpdateDesignRequest(UUID.fromString(uuid), json);
    }
}
