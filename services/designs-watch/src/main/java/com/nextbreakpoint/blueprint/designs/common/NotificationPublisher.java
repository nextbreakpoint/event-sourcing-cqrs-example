package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import rx.Single;

import java.util.Objects;

public class NotificationPublisher {
    private final EventBusAdapter adapter;

    public NotificationPublisher(EventBusAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter);
    }

    public Single<Void> publish(DesignChangedNotification notification) {
        return Single.fromCallable(() -> {
            adapter.publishDesignChangedNotification(notification);
            return null;
        });
    }
}
