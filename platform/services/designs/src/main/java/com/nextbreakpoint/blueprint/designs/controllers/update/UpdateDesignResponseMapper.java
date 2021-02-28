package com.nextbreakpoint.blueprint.designs.controllers.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.UpdateDesignResponse;
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
