package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResponse;
import io.vertx.core.json.Json;

public class DeleteDesignsOutputMapper implements Mapper<DeleteDesignsResponse, Content> {
    @Override
    public Content transform(DeleteDesignsResponse response) {
        return new Content(Json.encode(response));
    }
}
