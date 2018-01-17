package com.nextbreakpoint.shop.designs.insert;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class InsertDesignRequestMapper implements RequestMapper<InsertDesignRequest> {
    @Override
    public InsertDesignRequest apply(RoutingContext context) {
        final JsonObject bodyAsJson = context.getBodyAsJson();

        final JsonObject jsonObject = new JsonObject()
                .put("manifest", bodyAsJson.getString("manifest"))
                .put("metadata", bodyAsJson.getString("metadata"))
                .put("script", bodyAsJson.getString("script"));

        return new InsertDesignRequest(UUID.randomUUID(), jsonObject.encode());
    }
}
