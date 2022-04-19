package com.nextbreakpoint.blueprint.designs.operations.validate;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ValidateDesignRequestMapper implements Mapper<RoutingContext, ValidateDesignRequest> {
    @Override
    public ValidateDesignRequest transform(RoutingContext context) {
        final JsonObject json = context.getBodyAsJson();

        final String manifest = json.getString("manifest");

        if (manifest == null) {
            throw new IllegalStateException("the required parameter manifest is missing");
        }

        final String metadata = json.getString("metadata");

        if (metadata == null) {
            throw new IllegalStateException("the required parameter metadata is missing");
        }

        final String script = json.getString("script");

        if (script == null) {
            throw new IllegalStateException("the required parameter script is missing");
        }

        return ValidateDesignRequest.builder()
                .withManifest(manifest)
                .withMetadata(metadata)
                .withScript(script)
                .build();
    }
}
