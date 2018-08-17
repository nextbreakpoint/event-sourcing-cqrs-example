package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResult;
import io.vertx.core.json.Json;

public class DeleteDesignsOutputMapper implements Mapper<DeleteDesignsResult, Content> {
    @Override
    public Content transform(DeleteDesignsResult response) {
        return new Content(Json.encode(response));
    }
}