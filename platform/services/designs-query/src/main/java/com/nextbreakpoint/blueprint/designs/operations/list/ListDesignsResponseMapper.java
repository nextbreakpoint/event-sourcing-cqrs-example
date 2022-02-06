package com.nextbreakpoint.blueprint.designs.operations.list;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.persistence.ListDesignsResponse;
import io.vertx.core.json.Json;

public class ListDesignsResponseMapper implements Mapper<ListDesignsResponse, String> {
    @Override
    public String transform(ListDesignsResponse response) {
        return Json.encode(response.getDesigns());
    }
}
