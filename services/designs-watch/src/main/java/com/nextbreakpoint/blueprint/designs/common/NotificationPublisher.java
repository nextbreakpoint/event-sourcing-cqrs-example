package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import io.vertx.rxjava.core.Vertx;
import rx.Single;

import java.util.Objects;

public class NotificationPublisher {
    private final Vertx vertx;
    private final String address;

    public NotificationPublisher(Vertx vertx, String address) {
        this.vertx = Objects.requireNonNull(vertx);
        this.address = Objects.requireNonNull(address);
    }

    public Single<Void> publish(DesignChangedNotification notification) {
        return Single.fromCallable(() -> Json.encodeValue(notification))
                .map(payload -> vertx.eventBus().send(address, payload))
                .map(ignored -> null);
    }
}
