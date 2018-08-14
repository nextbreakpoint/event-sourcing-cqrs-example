package com.nextbreakpoint.shop.designs.delete;

import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import io.vertx.core.json.JsonObject;

public class DeleteDesignResponseMapper implements ResponseMapper<DeleteDesignResponse> {
    @Override
    public Result apply(DeleteDesignResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Result(json);
    }
}
