package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.Json;

public class InsertDesignResponseMapper implements Mapper<InsertDesignResponse, String> {
    @Override
    public String transform(InsertDesignResponse result) {
        return Json.encode(result);
    }
}
