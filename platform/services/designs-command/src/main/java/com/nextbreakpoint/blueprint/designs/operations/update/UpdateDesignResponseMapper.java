package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class UpdateDesignResponseMapper implements Mapper<UpdateDesignResponse, String> {
    @Override
    public String transform(UpdateDesignResponse result) {
        return Json.encodeValue(result);
    }
}
