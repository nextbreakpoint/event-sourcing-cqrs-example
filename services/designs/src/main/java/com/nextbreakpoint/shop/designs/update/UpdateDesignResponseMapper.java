package com.nextbreakpoint.shop.designs.update;

import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import io.vertx.core.json.JsonObject;

public class UpdateDesignResponseMapper implements ResponseMapper<UpdateDesignResponse> {
    @Override
    public Result apply(UpdateDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Result(json);
    }
}
