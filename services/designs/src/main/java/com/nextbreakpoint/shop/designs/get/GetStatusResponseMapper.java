package com.nextbreakpoint.shop.designs.get;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.designs.model.Status;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GetStatusResponseMapper implements Mapper<GetStatusResponse, Content> {
    @Override
    public Content transform(GetStatusResponse response) {
        final Status status = response.getStatus();

        final String json = new JsonArray()
                .add(convert(status))
                .encode();

        return new Content(json);
    }

    private JsonObject convert(Status status) {
        return new JsonObject()
                    .put("name", status.getName())
                    .put("updated", status.getUpdated());
    }
}
