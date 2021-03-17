package com.nextbreakpoint.blueprint.designs.controllers.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.command.InsertDesign;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class InsertDesignInputMapper implements Mapper<RoutingContext, InsertDesign> {
    @Override
    public InsertDesign transform(RoutingContext context) {
        final JsonObject bodyAsJson = context.getBodyAsJson();

        if (bodyAsJson == null) {
            throw new IllegalStateException("body is not defined in routing context");
        }

        final String manifest = bodyAsJson.getString("manifest");
        final String metadata = bodyAsJson.getString("metadata");
        final String script = bodyAsJson.getString("script");

        if (manifest == null || metadata == null || script == null) {
            throw new IllegalArgumentException("body doesn't contain required properties: manifest, metadata, script");
        }

        final String json = new JsonObject()
                .put("manifest", manifest)
                .put("metadata", metadata)
                .put("script", script)
                .encode();

        return new InsertDesign(UUID.randomUUID(), json, String.valueOf(System.currentTimeMillis()));
    }
}
