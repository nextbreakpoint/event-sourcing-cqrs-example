package com.nextbreakpoint.blueprint.accounts.operations.load;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class LoadSelfAccountRequestMapper implements Mapper<RoutingContext, LoadAccountRequest> {
    @Override
    public LoadAccountRequest transform(RoutingContext context) {
        if (context.user() == null) {
            throw new IllegalStateException("the user is not authenticated");
        }

        final String uuid = context.user().principal().getString("user");

        if (uuid == null) {
            throw new IllegalStateException("the user id is not defined");
        }

        try {
            return new LoadAccountRequest(UUID.fromString(uuid));
        } catch (Exception e) {
            throw new IllegalStateException("invalid request: " + e.getMessage());
        }
    }
}
