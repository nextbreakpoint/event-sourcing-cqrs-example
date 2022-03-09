package com.nextbreakpoint.blueprint.designs.operations.download;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DownloadDesignRequestMapper implements Mapper<RoutingContext, DownloadDesignRequest> {
    @Override
    public DownloadDesignRequest transform(RoutingContext context) {
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

        return new DownloadDesignRequest(manifest, metadata, script);
    }
}
