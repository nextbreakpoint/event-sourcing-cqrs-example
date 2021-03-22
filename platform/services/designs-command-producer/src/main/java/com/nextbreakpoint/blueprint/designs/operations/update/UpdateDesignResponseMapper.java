package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.Json;

public class UpdateDesignResponseMapper implements Mapper<UpdateDesignResponse, String> {
    @Override
    public String transform(UpdateDesignResponse result) {
        return Json.encode(result);
    }
}
