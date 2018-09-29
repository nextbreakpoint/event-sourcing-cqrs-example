package com.nextbreakpoint.shop.designs.controllers;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import io.vertx.rxjava.core.Vertx;
import rx.Single;

import java.util.Objects;

public class DesignChangedController implements Controller<DesignChangedEvent, DesignChangedEvent> {
    private final Vertx vertx;
    private final String address;

    public DesignChangedController(Vertx vertx, String address) {
        this.vertx = Objects.requireNonNull(vertx);
        this.address = Objects.requireNonNull(address);
    }

    @Override
    public Single<DesignChangedEvent> onNext(DesignChangedEvent event) {
        vertx.eventBus().publish(address, event);
        return Single.just(event);
    }
}
