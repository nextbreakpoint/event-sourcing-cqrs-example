package com.nextbreakpoint.shop.designs.handlers.delete;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.core.json.JsonObject;

public class DeleteDesignResponseMapper implements Mapper<DeleteDesignResponse, Content> {
    @Override
    public Content transform(DeleteDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Content(json);
    }
}
