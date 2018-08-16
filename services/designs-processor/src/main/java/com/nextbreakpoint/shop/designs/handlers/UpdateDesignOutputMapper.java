package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import io.vertx.core.json.JsonObject;

public class UpdateDesignOutputMapper implements Mapper<UpdateDesignResponse, Content> {
    @Override
    public Content transform(UpdateDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Content(json);
    }
}
