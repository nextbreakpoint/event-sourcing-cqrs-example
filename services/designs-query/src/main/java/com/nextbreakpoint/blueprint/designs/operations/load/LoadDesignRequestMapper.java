package com.nextbreakpoint.blueprint.designs.operations.load;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignRequest;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class LoadDesignRequestMapper implements Mapper<RoutingContext, LoadDesignRequest> {
    @Override
    public LoadDesignRequest transform(RoutingContext context) {
        final HttpServerRequest request = context.request();

        final String uuidParam = request.getParam("designId");

        if (uuidParam == null) {
            throw new IllegalStateException("the required parameter designId is missing");
        }

        final String draftParam = request.getParam("draft", "false");

        try {
            final UUID uuid = UUID.fromString(uuidParam);

            final boolean draft = Boolean.parseBoolean(draftParam);

            return LoadDesignRequest.builder()
                    .withUuid(uuid)
                    .withDraft(draft)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("invalid request: " + e.getMessage());
        }
    }
}
