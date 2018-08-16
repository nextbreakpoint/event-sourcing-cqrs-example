package com.nextbreakpoint.shop.designs.list;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.designs.model.Status;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ListStatusResponseMapper implements Mapper<ListStatusResponse, Content> {
    @Override
    public Content transform(ListStatusResponse response) {
        final String json = response.getValues()
                .stream()
                .map(this::convert)
                .collect(() -> new JsonArray(), (a, x) -> a.add(x), (a, b) -> a.addAll(b))
                .encode();

        return new Content(json);
    }

    private JsonObject convert(Status status) {
        return new JsonObject()
                .put("name", status.getName())
                .put("updated", status.getUpdated());
    }
}
