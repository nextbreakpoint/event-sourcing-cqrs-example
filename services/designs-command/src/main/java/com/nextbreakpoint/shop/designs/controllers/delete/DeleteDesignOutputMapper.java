package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResult;
import io.vertx.core.json.Json;

public class DeleteDesignOutputMapper implements Mapper<DeleteDesignResult, Content> {
    @Override
    public Content transform(DeleteDesignResult response) {
        return new Content(Json.encode(response));
    }
}
