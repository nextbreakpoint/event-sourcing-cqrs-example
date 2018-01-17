package com.nextbreakpoint.shop.designs.get;

import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import com.nextbreakpoint.shop.designs.model.Status;
import io.vertx.core.json.JsonObject;

public class GetStatusResponseMapper implements ResponseMapper<GetStatusResponse> {
    @Override
    public Result apply(GetStatusResponse response) {
        final Status status = response.getStatus();

        final String json = new JsonObject()
                .put("name", status.getName())
                .put("date", status.getDate())
                .encode();

        return new Result(json);
    }

}
