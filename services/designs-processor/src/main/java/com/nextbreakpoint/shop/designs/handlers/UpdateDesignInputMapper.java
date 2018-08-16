package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.designs.model.UpdateDesignRequest;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

public class UpdateDesignInputMapper implements Mapper<Message, UpdateDesignRequest> {
    @Override
    public UpdateDesignRequest transform(Message message) {
        final JsonObject jsonObject = JsonObject.mapFrom(message);
        final String uuid = jsonObject.getString("uuid");
        final String json = jsonObject.getString("json");
        return new UpdateDesignRequest(UUID.fromString(uuid), json);
    }
}
