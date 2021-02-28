package com.nextbreakpoint.blueprint.designs.controllers.delete;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.DeleteDesignResponse;
import io.vertx.core.json.JsonObject;

public class DeleteDesignResponseMapper implements Mapper<DeleteDesignResponse, String> {
    @Override
    public String transform(DeleteDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return json;
    }
}
