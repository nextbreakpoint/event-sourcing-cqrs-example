package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class InsertDesignRequestMapper implements Mapper<RoutingContext, InsertDesignRequest> {
    @Override
    public InsertDesignRequest transform(RoutingContext context) {
        final JsonObject bodyAsJson = context.getBodyAsJson();

        if (bodyAsJson == null) {
            throw new IllegalStateException("the request's body is not defined");
        }

        final String manifest = bodyAsJson.getString("manifest");
        final String metadata = bodyAsJson.getString("metadata");
        final String script = bodyAsJson.getString("script");

        if (manifest == null || metadata == null || script == null) {
            throw new IllegalArgumentException("the request's body doesn't contain the required properties: manifest, metadata, script");
        }

        final String json = new JsonObject()
                .put("manifest", manifest)
                .put("metadata", metadata)
                .put("script", script)
                .encode();

        final JsonObject principal = context.user().principal();

        final UUID owner = UUID.fromString(principal.getString("user"));

        return new InsertDesignRequest(owner, UUID.randomUUID(), UUID.randomUUID(), json);
    }
}
