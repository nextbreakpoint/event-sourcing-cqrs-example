package com.nextbreakpoint.blueprint.accounts.operations.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class InsertAccountRequestMapper implements Mapper<RoutingContext, InsertAccountRequest> {
    @Override
    public InsertAccountRequest transform(RoutingContext context) {
        if (context.body() == null) {
            throw new IllegalStateException("the request's body is empty");
        }

        final JsonObject bodyAsJson = context.body().asJsonObject();

        final String email = bodyAsJson.getString("email");

        if (email == null) {
            throw new IllegalStateException("the request's body doesn't contain the required properties: email is missing");
        }

        final String name = bodyAsJson.getString("name");

        if (name == null) {
            throw new IllegalStateException("the request's body doesn't contain the required properties: name is missing");
        }

        final String role = bodyAsJson.getString("role");

        if (role == null) {
            throw new IllegalStateException("the request's body doesn't contain the required properties: role is missing");
        }

        final UUID uuid = UUID.randomUUID();

        return new InsertAccountRequest(uuid, name, email, role);
    }
}
