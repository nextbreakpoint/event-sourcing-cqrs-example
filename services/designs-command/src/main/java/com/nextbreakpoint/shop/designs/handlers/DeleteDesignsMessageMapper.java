package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.common.MessageType;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsEvent;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignsMessageMapper implements Mapper<DeleteDesignsEvent, Message> {
    private final String messageSource;

    public DeleteDesignsMessageMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public Message transform(DeleteDesignsEvent request) {
        return new Message(UUID.randomUUID().toString(), MessageType.DESIGNS_DELETE, Json.encode(request), messageSource, new UUID(0,0).toString(), System.currentTimeMillis());
    }
}
