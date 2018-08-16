package com.nextbreakpoint.shop.designs.handlers.update;

import com.nextbreakpoint.shop.common.Mapper;
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
