package com.nextbreakpoint.shop.designs.controllers.update;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.CommandResult;
import io.vertx.core.json.Json;

public class UpdateDesignOutputMapper implements Mapper<CommandResult, String> {
    @Override
    public String transform(CommandResult result) {
        return Json.encode(result);
    }
}
