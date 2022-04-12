package com.nextbreakpoint.blueprint.accounts.operations.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class InsertAccountRequestMapper implements Mapper<RoutingContext, InsertAccountRequest> {
    @Override
    public InsertAccountRequest transform(RoutingContext context) {
        final UUID uuid = UUID.randomUUID();

        final JsonObject bodyAsJson = context.getBodyAsJson();
        final String email = bodyAsJson.getString("email");
        final String name = bodyAsJson.getString("name");
        final String role = bodyAsJson.getString("role");

        return new InsertAccountRequest(uuid, name, email, role);
    }
}
