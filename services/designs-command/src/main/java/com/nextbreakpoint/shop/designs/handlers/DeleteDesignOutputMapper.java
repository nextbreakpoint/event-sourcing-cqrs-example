package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResult;
import io.vertx.core.json.Json;

public class DeleteDesignOutputMapper implements Mapper<DeleteDesignResult, Content> {
    @Override
    public Content transform(DeleteDesignResult response) {
        return new Content(Json.encode(response));
    }
}
