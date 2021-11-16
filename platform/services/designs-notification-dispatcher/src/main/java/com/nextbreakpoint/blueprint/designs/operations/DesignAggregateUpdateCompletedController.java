package com.nextbreakpoint.blueprint.designs.operations;

import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.Vertx;
import rx.Single;

import java.util.Objects;

public class DesignAggregateUpdateCompletedController implements Controller<DesignAggregateUpdateCompleted, Void> {
    private final Vertx vertx;
    private final String address;

    public DesignAggregateUpdateCompletedController(Vertx vertx, String address) {
        this.vertx = Objects.requireNonNull(vertx);
        this.address = Objects.requireNonNull(address);
    }

    @Override
    public Single<Void> onNext(DesignAggregateUpdateCompleted event) {
        DesignChangedNotification notification = new DesignChangedNotification(event.getUuid().toString(), event.getEvid().timestamp());
        vertx.eventBus().publish(address, Json.encode(notification));
        return Single.just(null);
    }
}
