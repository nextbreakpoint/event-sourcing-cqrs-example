package com.nextbreakpoint.shop.designs.controllers.list;

import com.nextbreakpoint.shop.common.model.DesignDocument;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ListDesignsResponseMapper implements Mapper<ListDesignsResponse, String> {
    @Override
    public String transform(ListDesignsResponse response) {
        final String json = response.getDocuments()
                .stream()
                .map(document -> createObject(document))
                .collect(() -> new JsonArray(), (a, x) -> a.add(x), (a, b) -> a.addAll(b))
                .encode();

        return json;
    }

    private JsonObject createObject(DesignDocument document) {
        return new JsonObject().put("uuid", document.getUuid()).put("checksum", document.getChecksum());
    }
}
