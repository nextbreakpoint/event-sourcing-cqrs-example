package com.nextbreakpoint.shop.designs.controllers.changed;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import io.vertx.core.json.JsonObject;

public class DesignChangedOutputMapper implements Mapper<DesignChangedEvent, JsonObject> {
    @Override
    public JsonObject transform(DesignChangedEvent event) {
        final JsonObject object = new JsonObject();
        object.put("uuid", event.getUuid().toString());
        object.put("timestamp", event.getTimestamp());
        return object;
    }
}
