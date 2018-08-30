package com.nextbreakpoint.shop.common.vertx.handlers;

import com.nextbreakpoint.shop.common.model.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;

import java.util.Objects;
import java.util.function.BiConsumer;

public class EventBusConsumer implements BiConsumer<Message, JsonObject> {
    private final Logger logger = LoggerFactory.getLogger(EventBusConsumer.class.getName());

    private final Vertx vertx;
    private final String address;

    public EventBusConsumer(Vertx vertx, String address) {
        this.vertx = Objects.requireNonNull(vertx);
        this.address = Objects.requireNonNull(address);
    }

    @Override
    public void accept(Message message, JsonObject object) {
        vertx.eventBus().publish(address, object);
        logger.info("Message processed: id=" + message.getMessageId());
    }
}
