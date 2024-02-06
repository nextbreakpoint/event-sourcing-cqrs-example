package com.nextbreakpoint.blueprint.designs.operations.render;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class RenderDesignRequestMapper implements Mapper<RoutingContext, RenderDesignRequest> {
    @Override
    public RenderDesignRequest transform(RoutingContext context) {
        if (context.body() == null) {
            throw new IllegalStateException("the request's body is empty");
        }

        final JsonObject bodyAsJson = context.body().asJsonObject();

        final String manifest = bodyAsJson.getString("manifest");

        if (manifest == null) {
            throw new IllegalStateException("the request's body doesn't contain the required properties: manifest is missing");
        }

        final String metadata = bodyAsJson.getString("metadata");

        if (metadata == null) {
            throw new IllegalStateException("the request's body doesn't contain the required properties: metadata is missing");
        }

        final String script = bodyAsJson.getString("script");

        if (script == null) {
            throw new IllegalStateException("the request's body doesn't contain the required properties: script is missing");
        }

        return RenderDesignRequest.builder()
                .withManifest(manifest)
                .withMetadata(metadata)
                .withScript(script)
                .build();
    }
}
