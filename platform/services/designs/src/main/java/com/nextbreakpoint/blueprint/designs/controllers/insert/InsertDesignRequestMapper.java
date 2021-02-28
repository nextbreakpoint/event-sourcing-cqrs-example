package com.nextbreakpoint.blueprint.designs.controllers.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.InsertDesignRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class InsertDesignRequestMapper implements Mapper<RoutingContext, InsertDesignRequest> {
    @Override
    public InsertDesignRequest transform(RoutingContext context) {
        final JsonObject bodyAsJson = context.getBodyAsJson();

        final String json = new JsonObject()
                .put("manifest", bodyAsJson.getString("manifest"))
                .put("metadata", bodyAsJson.getString("metadata"))
                .put("script", bodyAsJson.getString("script"))
                .encode();

        return new InsertDesignRequest(UUID.randomUUID(), json);
    }
}
