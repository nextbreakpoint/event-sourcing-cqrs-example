package com.nextbreakpoint.shop.designs.controllers.update;

import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResult;
import io.vertx.core.json.Json;

public class UpdateDesignOutputMapper implements Mapper<UpdateDesignResult, Content> {
    @Override
    public Content transform(UpdateDesignResult result) {
        return new Content(Json.encode(result));
    }
}
