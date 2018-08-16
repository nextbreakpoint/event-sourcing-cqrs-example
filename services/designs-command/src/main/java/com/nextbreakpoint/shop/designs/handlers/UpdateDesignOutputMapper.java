package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import io.vertx.core.json.Json;

public class UpdateDesignOutputMapper implements Mapper<UpdateDesignResponse, Content> {
    @Override
    public Content transform(UpdateDesignResponse response) {
        return new Content(Json.encode(response));
    }
}
