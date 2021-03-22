package com.nextbreakpoint.blueprint.designs.operations;

import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.Vertx;
import rx.Single;

import java.util.Objects;

public class DesignChangedController implements Controller<DesignChanged, DesignChanged> {
    private final Vertx vertx;
    private final String address;

    public DesignChangedController(Vertx vertx, String address) {
        this.vertx = Objects.requireNonNull(vertx);
        this.address = Objects.requireNonNull(address);
    }

    @Override
    public Single<DesignChanged> onNext(DesignChanged event) {
        vertx.eventBus().publish(address, Json.encode(event));
        return Single.just(event);
    }
}
