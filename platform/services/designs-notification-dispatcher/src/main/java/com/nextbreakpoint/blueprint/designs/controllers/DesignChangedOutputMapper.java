package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import io.vertx.core.json.JsonObject;

public class DesignChangedOutputMapper implements Mapper<DesignChanged, JsonObject> {
    @Override
    public JsonObject transform(DesignChanged event) {
        final JsonObject object = new JsonObject();
        object.put("uuid", event.getUuid().toString());
        object.put("timestamp", event.getTimestamp());
        return object;
    }
}
