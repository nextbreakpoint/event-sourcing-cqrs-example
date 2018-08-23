package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResult;
import io.vertx.core.json.Json;

public class DeleteDesignsOutputMapper implements Mapper<DeleteDesignsResult, Content> {
    @Override
    public Content transform(DeleteDesignsResult result) {
        return new Content(Json.encode(result));
    }
}
