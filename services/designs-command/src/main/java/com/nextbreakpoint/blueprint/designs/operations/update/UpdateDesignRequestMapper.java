package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class UpdateDesignRequestMapper implements Mapper<RoutingContext, UpdateDesignRequest> {
    @Override
    public UpdateDesignRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("designId");

        if (uuid == null) {
            throw new IllegalStateException("the required parameter designId is missing");
        }

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

        final Boolean published = bodyAsJson.getBoolean("published", false);

        final String json = new JsonObject()
                .put("manifest", manifest)
                .put("metadata", metadata)
                .put("script", script)
                .encode();

        final JsonObject principal = context.user().principal();

        final UUID owner = UUID.fromString(principal.getString("user"));

        return UpdateDesignRequest.builder()
                .withOwner(owner)
                .withUuid(UUID.fromString(uuid))
                .withChange(UUID.randomUUID())
                .withJson(json)
                .withPublished(published)
                .build();
    }
}