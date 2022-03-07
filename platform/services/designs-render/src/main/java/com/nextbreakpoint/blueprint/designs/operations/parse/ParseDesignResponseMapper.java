package com.nextbreakpoint.blueprint.designs.operations.parse;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class ParseDesignResponseMapper implements Mapper<ParseDesignResponse, String> {
    @Override
    public String transform(ParseDesignResponse result) {
        return Json.encodeValue(result);
    }
}
