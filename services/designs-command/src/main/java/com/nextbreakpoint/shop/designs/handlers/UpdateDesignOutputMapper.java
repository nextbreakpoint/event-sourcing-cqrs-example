package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResult;
import io.vertx.core.json.Json;

public class UpdateDesignOutputMapper implements Mapper<UpdateDesignResult, Content> {
    @Override
    public Content transform(UpdateDesignResult response) {
        return new Content(Json.encode(response));
    }
}
