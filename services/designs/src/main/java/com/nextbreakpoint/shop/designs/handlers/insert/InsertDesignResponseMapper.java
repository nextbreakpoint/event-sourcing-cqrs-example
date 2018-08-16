package com.nextbreakpoint.shop.designs.handlers.insert;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;
import io.vertx.core.json.JsonObject;

public class InsertDesignResponseMapper implements Mapper<InsertDesignResponse, Content> {
    @Override
    public Content transform(InsertDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Content(json);
    }
}
