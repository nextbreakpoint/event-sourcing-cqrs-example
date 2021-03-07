package com.nextbreakpoint.blueprint.designs.controllers.update;

import com.datastax.driver.core.utils.UUIDs;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class UpdateDesignInputMapper implements Mapper<RoutingContext, UpdateDesign> {
    @Override
    public UpdateDesign transform(RoutingContext context) {
        final String uuid = context.request().getParam("designId");

        if (uuid == null) {
            throw new IllegalStateException("parameter uuid (designId) missing from routing context");
        }

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

        return new UpdateDesign(UUID.fromString(uuid), json, UUIDs.timeBased());
    }
}
