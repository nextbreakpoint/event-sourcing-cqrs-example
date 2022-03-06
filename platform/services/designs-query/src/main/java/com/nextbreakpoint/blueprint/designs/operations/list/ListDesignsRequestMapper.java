package com.nextbreakpoint.blueprint.designs.operations.list;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsRequest;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ListDesignsRequestMapper implements Mapper<RoutingContext, ListDesignsRequest> {
    @Override
    public ListDesignsRequest transform(RoutingContext context) {
        final HttpServerRequest request = context.request();

        final String draftParam = request.getParam("draft", "false");

        try {
            final boolean draft = Boolean.parseBoolean(draftParam);

            return new ListDesignsRequest(draft);
        } catch (Exception e) {
            throw new IllegalStateException("invalid parameters: " + e.getMessage());
        }
    }
}
