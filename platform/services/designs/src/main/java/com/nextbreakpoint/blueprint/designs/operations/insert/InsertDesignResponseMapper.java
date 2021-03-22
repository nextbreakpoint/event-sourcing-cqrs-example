package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;

public class InsertDesignResponseMapper implements Mapper<InsertDesignResponse, String> {
    @Override
    public String transform(InsertDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return json;
    }
}
