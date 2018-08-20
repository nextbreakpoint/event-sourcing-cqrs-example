package com.nextbreakpoint.shop.designs.controllers.update;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import io.vertx.core.json.JsonObject;

public class UpdateDesignResponseMapper implements Mapper<UpdateDesignResponse, Content> {
    @Override
    public Content transform(UpdateDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Content(json);
    }
}
