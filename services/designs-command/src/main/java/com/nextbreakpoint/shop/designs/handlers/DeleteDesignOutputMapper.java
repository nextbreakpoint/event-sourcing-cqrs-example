package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import io.vertx.core.json.Json;

public class DeleteDesignOutputMapper implements Mapper<DeleteDesignResponse, Content> {
    @Override
    public Content transform(DeleteDesignResponse response) {
        return new Content(Json.encode(response));
    }
}
