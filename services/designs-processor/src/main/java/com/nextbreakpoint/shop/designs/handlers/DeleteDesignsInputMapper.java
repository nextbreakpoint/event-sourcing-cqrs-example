package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsRequest;
import io.vertx.core.json.JsonObject;

public class DeleteDesignsInputMapper implements Mapper<Message, DeleteDesignsRequest> {
    @Override
    public DeleteDesignsRequest transform(Message message) {
        final JsonObject jsonObject = JsonObject.mapFrom(message);
        return new DeleteDesignsRequest();
    }
}
