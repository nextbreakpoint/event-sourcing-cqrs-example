package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.Json;

public class DeleteDesignResponseMapper implements Mapper<DeleteDesignResponse, String> {
    @Override
    public String transform(DeleteDesignResponse result) {
        return Json.encode(result);
    }
}
