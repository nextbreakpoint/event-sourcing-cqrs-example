package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import io.vertx.core.json.JsonObject;

public class DeleteDesignOutputMapper implements Mapper<DeleteDesignResponse, Content> {
    @Override
    public Content transform(DeleteDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Content(json);
    }
}
