package com.nextbreakpoint.shop.designs.insert;

import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import io.vertx.core.json.JsonObject;

public class InsertDesignResponseMapper implements ResponseMapper<InsertDesignResponse> {
    @Override
    public Result apply(InsertDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Result(json);
    }
}
