package com.nextbreakpoint.shop.designs.controllers.insert;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
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
