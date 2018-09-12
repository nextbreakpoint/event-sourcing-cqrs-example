package com.nextbreakpoint.shop.designs.controllers.list;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import io.vertx.core.json.Json;

public class ListDesignsOutputMapper implements Mapper<ListDesignsResponse, String> {
    @Override
    public String transform(ListDesignsResponse response) {
        return Json.encode(response.getDocuments());
    }
}
