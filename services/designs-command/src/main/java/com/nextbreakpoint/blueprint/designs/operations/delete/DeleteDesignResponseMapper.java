package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class DeleteDesignResponseMapper implements Mapper<DeleteDesignResponse, String> {
    @Override
    public String transform(DeleteDesignResponse result) {
        return Json.encodeValue(result);
    }
}
