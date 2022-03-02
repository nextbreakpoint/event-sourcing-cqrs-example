package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import io.vertx.rxjava.core.Vertx;
import rx.Single;

import java.util.Objects;

public class DesignDocumentUpdateCompletedController implements Controller<DesignDocumentUpdateCompleted, Void> {
    private final Vertx vertx;
    private final String address;

    public DesignDocumentUpdateCompletedController(Vertx vertx, String address) {
        this.vertx = Objects.requireNonNull(vertx);
        this.address = Objects.requireNonNull(address);
    }

    @Override
    public Single<Void> onNext(DesignDocumentUpdateCompleted event) {
        DesignChangedNotification notification = DesignChangedNotification.builder()
                .withKey(event.getDesignId().toString())
                .withRevision(event.getRevision())
                .build();

        vertx.eventBus().publish(address, Json.encodeValue(notification));

        return Single.just(null);
    }
}
