package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.common.vertx.TemplateHandler;
import com.nextbreakpoint.shop.common.vertx.consumers.MessaggeFailureConsumer;
import com.nextbreakpoint.shop.common.vertx.consumers.MessaggeSuccessConsumer;
import com.nextbreakpoint.shop.designs.controllers.DesignChangedInputMapper;
import com.nextbreakpoint.shop.designs.controllers.DesignChangedOutputMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class Factory {
    private Factory() {}

    public static Handler<Message> createDesignChangedHandler(Controller<DesignChangedEvent, DesignChangedEvent> controller) {
        return TemplateHandler.<Message, DesignChangedEvent, DesignChangedEvent, JsonObject>builder()
                .withInputMapper(new DesignChangedInputMapper())
                .withOutputMapper(new DesignChangedOutputMapper())
                .withController(controller)
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }
}
