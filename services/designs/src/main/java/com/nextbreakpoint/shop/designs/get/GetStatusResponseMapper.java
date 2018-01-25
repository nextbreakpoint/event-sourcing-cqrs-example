package com.nextbreakpoint.shop.designs.get;

import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import com.nextbreakpoint.shop.designs.model.Status;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GetStatusResponseMapper implements ResponseMapper<GetStatusResponse> {
    @Override
    public Result apply(GetStatusResponse response) {
        final Status status = response.getStatus();

        final String json = new JsonArray()
                .add(convert(status))
                .encode();

        return new Result(json);
    }

    private JsonObject convert(Status status) {
        return new JsonObject()
                    .put("name", status.getName())
                    .put("updated", status.getUpdated());
    }
}
