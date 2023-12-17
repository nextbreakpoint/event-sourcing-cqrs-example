package com.nextbreakpoint.blueprint.accounts.operations.delete;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteAccountRequestMapper implements Mapper<RoutingContext, DeleteAccountRequest> {
    @Override
    public DeleteAccountRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("accountId");

        if (uuid == null) {
            throw new IllegalStateException("the required parameter accountId is missing");
        }

        try {
            return new DeleteAccountRequest(UUID.fromString(uuid));
        } catch (Exception e) {
            throw new IllegalStateException("invalid request: " + e.getMessage());
        }
    }
}
