package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class UpdateDesignRequestMapper implements Mapper<RoutingContext, UpdateDesignRequest> {
    @Override
    public UpdateDesignRequest transform(RoutingContext context) {
        final String uuidParam = context.request().getParam("designId");

        if (uuidParam == null) {
            throw new IllegalStateException("the required parameter designId is missing");
        }

        if (context.user() == null) {
            throw new IllegalStateException("the user is not authenticated");
        }

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

        final Boolean published = bodyAsJson.getBoolean("published", false);

        final String json = new JsonObject()
                .put("manifest", manifest)
                .put("metadata", metadata)
                .put("script", script)
                .encode();

        final JsonObject principal = context.user().principal();

        try {
            final UUID owner = UUID.fromString(principal.getString("user"));

            final UUID uuid = UUID.fromString(uuidParam);

            return UpdateDesignRequest.builder()
                    .withOwner(owner)
                    .withUuid(uuid)
                    .withChange(UUID.randomUUID())
                    .withJson(json)
                    .withPublished(published)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("invalid request: " + e.getMessage());
        }
    }
}
