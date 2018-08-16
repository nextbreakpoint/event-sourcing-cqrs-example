package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import io.vertx.core.json.Json;

public class InsertDesignOutputMapper implements Mapper<InsertDesignResponse, Content> {
    @Override
    public Content transform(InsertDesignResponse response) {
        return new Content(Json.encode(response));
    }
}
