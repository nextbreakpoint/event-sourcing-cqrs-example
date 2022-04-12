package com.nextbreakpoint.blueprint.designs.operations.validate;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class ValidateDesignResponseMapper implements Mapper<ValidateDesignResponse, String> {
    @Override
    public String transform(ValidateDesignResponse response) {
        return Json.encodeValue(response);
    }
}
