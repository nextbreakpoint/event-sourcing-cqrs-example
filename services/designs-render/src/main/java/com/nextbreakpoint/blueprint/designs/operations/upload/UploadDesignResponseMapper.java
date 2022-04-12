package com.nextbreakpoint.blueprint.designs.operations.upload;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class UploadDesignResponseMapper implements Mapper<UploadDesignResponse, String> {
    @Override
    public String transform(UploadDesignResponse response) {
        return Json.encodeValue(response);
    }
}
