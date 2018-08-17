package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.designs.model.InsertDesignResult;
import io.vertx.core.json.Json;

public class InsertDesignOutputMapper implements Mapper<InsertDesignResult, Content> {
    @Override
    public Content transform(InsertDesignResult response) {
        return new Content(Json.encode(response));
    }
}
