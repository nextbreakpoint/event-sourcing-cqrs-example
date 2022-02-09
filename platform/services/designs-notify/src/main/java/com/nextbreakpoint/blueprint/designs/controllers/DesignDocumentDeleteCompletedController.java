package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import io.vertx.rxjava.core.Vertx;
import rx.Single;

import java.util.Objects;

public class DesignDocumentDeleteCompletedController implements Controller<DesignDocumentDeleteCompleted, Void> {
    private final Vertx vertx;
    private final String address;

    public DesignDocumentDeleteCompletedController(Vertx vertx, String address) {
        this.vertx = Objects.requireNonNull(vertx);
        this.address = Objects.requireNonNull(address);
    }

    @Override
    public Single<Void> onNext(DesignDocumentDeleteCompleted event) {
        DesignChangedNotification notification = DesignChangedNotification.builder()
                .withKey(event.getUuid().toString())
                .withTimestamp(event.getEvid().timestamp())
                .build();
        vertx.eventBus().publish(address, Json.encodeValue(notification));
        return Single.just(null);
    }
}
