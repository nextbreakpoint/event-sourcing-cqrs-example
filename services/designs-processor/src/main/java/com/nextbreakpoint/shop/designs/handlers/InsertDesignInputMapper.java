package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.designs.model.InsertDesignRequest;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

public class InsertDesignInputMapper implements Mapper<Message, InsertDesignRequest> {
    @Override
    public InsertDesignRequest transform(Message message) {
        final JsonObject jsonObject = JsonObject.mapFrom(message);
        final String uuid = jsonObject.getString("uuid");
        final String json = jsonObject.getString("json");
        return new InsertDesignRequest(UUID.fromString(uuid), json);
    }
}
