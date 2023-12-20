package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.designs.common.NotificationPublisher;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import rx.Single;

import java.util.Objects;

public class DesignDocumentUpdateCompletedController implements Controller<InputMessage<DesignDocumentUpdateCompleted>, Void> {
    private final NotificationPublisher publisher;

    public DesignDocumentUpdateCompletedController(NotificationPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher);
    }

    @Override
    public Single<Void> onNext(InputMessage<DesignDocumentUpdateCompleted> message) {
        return Single.just(message.getValue().getData())
                .map(this::createNotification)
                .flatMap(this::publishNotification);
    }

    private DesignChangedNotification createNotification(DesignDocumentUpdateCompleted event) {
        return DesignChangedNotification.builder()
                .withKey(event.getDesignId().toString())
                .withRevision(event.getRevision())
                .build();
    }

    private Single<Void> publishNotification(DesignChangedNotification notification) {
        return publisher.publish(notification);
    }
}
