package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.designs.common.NotificationPublisher;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import rx.Single;

import java.util.Objects;

public class DesignDocumentDeleteCompletedController implements Controller<InputMessage<DesignDocumentDeleteCompleted>, Void> {
    private final NotificationPublisher publisher;

    public DesignDocumentDeleteCompletedController(NotificationPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher);
    }

    @Override
    public Single<Void> onNext(InputMessage<DesignDocumentDeleteCompleted> message) {
        return Single.just(message.getValue().getData())
                .map(this::createNotification)
                .flatMap(this::publishNotification);
    }

    private DesignChangedNotification createNotification(DesignDocumentDeleteCompleted event) {
        return DesignChangedNotification.builder()
                .withKey(event.getDesignId().toString())
                .withRevision(event.getRevision())
                .build();
    }

    private Single<Void> publishNotification(DesignChangedNotification notification) {
        return publisher.publish(notification);
    }
}
