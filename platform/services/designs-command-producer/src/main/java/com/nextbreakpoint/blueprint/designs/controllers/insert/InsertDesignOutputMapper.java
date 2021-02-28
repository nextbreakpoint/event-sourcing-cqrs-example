package com.nextbreakpoint.blueprint.designs.controllers.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.CommandResult;
import io.vertx.core.json.Json;

public class InsertDesignOutputMapper implements Mapper<CommandResult, String> {
    @Override
    public String transform(CommandResult result) {
        return Json.encode(result);
    }
}
