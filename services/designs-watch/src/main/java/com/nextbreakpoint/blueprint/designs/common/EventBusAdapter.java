package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import com.nextbreakpoint.blueprint.designs.model.SessionUpdatedNotification;
import io.vertx.rxjava.core.Vertx;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;
import java.util.function.Consumer;

@Log4j2
public class EventBusAdapter {
    private final Vertx vertx;

    public EventBusAdapter(Vertx vertx) {
        this.vertx = Objects.requireNonNull(vertx);
    }

    public MessageConsumerAdapter registerDesignChangeNotificationConsumer(Consumer<DesignChangedNotification> consumer) {
        return new MessageConsumerAdapter(vertx.eventBus().consumer("notifications", message -> {
            try {
                consumer.accept(Json.decodeValue(message.body(), DesignChangedNotification.class));
            } catch (Exception e) {
                log.error("Failed to process notification message", e);
            }
        }));
    }

    public void publishDesignChangedNotification(DesignChangedNotification notification) {
        vertx.eventBus().publish("notifications", Json.encodeValue(notification));
    }

    public MessageConsumerAdapter registerSessionUpdateNotificationConsumer(String sessionId, Consumer<SessionUpdatedNotification> consumer) {
        return new MessageConsumerAdapter(vertx.eventBus().consumer("notifications." + sessionId, message -> {
            try {
                consumer.accept(Json.decodeValue(message.body(), SessionUpdatedNotification.class));
            } catch (Exception e) {
                log.error("Failed to process notification message", e);
            }
        }));
    }

    public void publishSessionUpdateNotification(String sessionId, SessionUpdatedNotification notification) {
        vertx.eventBus().publish("notifications." + sessionId, Json.encodeValue(notification));
    }

}
