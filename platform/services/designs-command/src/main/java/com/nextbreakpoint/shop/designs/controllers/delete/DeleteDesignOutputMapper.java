package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.CommandResult;
import io.vertx.core.json.Json;

public class DeleteDesignOutputMapper implements Mapper<CommandResult, String> {
    @Override
    public String transform(CommandResult result) {
        return Json.encode(result);
    }
}
