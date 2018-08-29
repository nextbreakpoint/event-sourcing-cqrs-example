package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.handlers.DefaultHandler;
import com.nextbreakpoint.shop.common.handlers.EventBusConsumer;
import com.nextbreakpoint.shop.common.handlers.FailedMessageConsumer;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.designs.controllers.DesignChangedController;
import com.nextbreakpoint.shop.designs.controllers.DesignChangedInputMapper;
import com.nextbreakpoint.shop.designs.controllers.DesignChangedOutputMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;


public class Factory {
    private Factory() {}

    public static Handler<Message> createDesignChangedHandler(Vertx vertx, String address) {
        return DefaultHandler.<Message, DesignChangedEvent, DesignChangedEvent, JsonObject>builder()
                .withInputMapper(new DesignChangedInputMapper())
                .withOutputMapper(new DesignChangedOutputMapper())
                .withController(new DesignChangedController())
                .onSuccess(new EventBusConsumer(vertx, address))
                .onFailure(new FailedMessageConsumer())
                .build();
    }
}
