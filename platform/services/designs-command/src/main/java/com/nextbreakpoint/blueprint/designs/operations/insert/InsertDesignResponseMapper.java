package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class InsertDesignResponseMapper implements Mapper<InsertDesignResponse, String> {
    @Override
    public String transform(InsertDesignResponse result) {
        return Json.encodeValue(result);
    }
}
