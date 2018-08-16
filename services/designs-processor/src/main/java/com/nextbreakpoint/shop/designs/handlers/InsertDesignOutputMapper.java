package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import io.vertx.core.json.JsonObject;

public class InsertDesignOutputMapper implements Mapper<InsertDesignResponse, Content> {
    @Override
    public Content transform(InsertDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Content(json);
    }
}
