package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;

public class UpdateDesignResponseMapper implements Mapper<UpdateDesignResponse, String> {
    @Override
    public String transform(UpdateDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return json;
    }
}
