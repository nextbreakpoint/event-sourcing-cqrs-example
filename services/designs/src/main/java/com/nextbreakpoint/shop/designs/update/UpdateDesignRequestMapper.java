package com.nextbreakpoint.shop.designs.update;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class UpdateDesignRequestMapper implements RequestMapper<UpdateDesignRequest> {
    @Override
    public UpdateDesignRequest apply(RoutingContext context) {
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
