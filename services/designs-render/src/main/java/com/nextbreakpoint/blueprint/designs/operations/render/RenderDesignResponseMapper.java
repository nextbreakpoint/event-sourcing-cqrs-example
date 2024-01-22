package com.nextbreakpoint.blueprint.designs.operations.render;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class RenderDesignResponseMapper implements Mapper<RenderDesignResponse, String> {
    @Override
    public String transform(RenderDesignResponse response) {
        return Json.encodeValue(response);
    }
}
