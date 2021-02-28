package com.nextbreakpoint.blueprint.designs.controllers.list;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.ListDesignsResponse;
import io.vertx.core.json.Json;

public class ListDesignsOutputMapper implements Mapper<ListDesignsResponse, String> {
    @Override
    public String transform(ListDesignsResponse response) {
        return Json.encode(response.getDocuments());
    }
}
