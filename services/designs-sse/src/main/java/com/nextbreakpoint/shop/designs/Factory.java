package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.vertx.TemplateHandler;
import com.nextbreakpoint.shop.common.vertx.consumers.EventBusConsumer;
import com.nextbreakpoint.shop.common.vertx.consumers.FailedMessageConsumer;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.designs.controllers.DesignChangedController;
import com.nextbreakpoint.shop.designs.controllers.DesignChangedInputMapper;
import com.nextbreakpoint.shop.designs.controllers.DesignChangedOutputMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.util.function.BiConsumer;

public class Factory {
    private Factory() {}

    public static Handler<Message> createDesignChangedHandler(BiConsumer<Message, JsonObject> consumer) {
        return TemplateHandler.<Message, DesignChangedEvent, DesignChangedEvent, JsonObject>builder()
                .withInputMapper(new DesignChangedInputMapper())
                .withOutputMapper(new DesignChangedOutputMapper())
                .withController(new DesignChangedController())
                .onSuccess(consumer)
                .onFailure(new FailedMessageConsumer())
                .build();
    }
}
