package com.nextbreakpoint.blueprint.designs.operations.list;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.DesignDocument;
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
