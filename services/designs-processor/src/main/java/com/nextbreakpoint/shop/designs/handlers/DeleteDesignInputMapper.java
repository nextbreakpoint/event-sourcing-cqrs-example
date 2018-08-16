package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.designs.model.DeleteDesignRequest;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

public class DeleteDesignInputMapper implements Mapper<Message, DeleteDesignRequest> {
    @Override
    public DeleteDesignRequest transform(Message message) {
        final JsonObject jsonObject = JsonObject.mapFrom(message);
        final String uuid = jsonObject.getString("uuid");
        return new DeleteDesignRequest(UUID.fromString(uuid));
    }
}
