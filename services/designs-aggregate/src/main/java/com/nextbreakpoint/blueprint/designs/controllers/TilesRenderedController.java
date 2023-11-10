package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateStatus;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.avro.TilesRendered;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.time.ZoneOffset;
import java.util.Objects;

public class TilesRenderedController implements Controller<InputMessage<TilesRendered>, Void> {
    private final String messageSource;
    private final MessageEmitter<DesignAggregateUpdated> emitter;
    private final DesignEventStore eventStore;

    public TilesRenderedController(String messageSource, DesignEventStore eventStore, MessageEmitter<DesignAggregateUpdated> emitter) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.eventStore = Objects.requireNonNull(eventStore);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage<TilesRendered> message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .map(receivedMessage -> receivedMessage.getValue().getData())
                .flatMapObservable(event -> onUpdateRequested(event, message.getToken()))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(value -> null);
    }

    private Single<InputMessage<TilesRendered>> onMessageReceived(InputMessage<TilesRendered> message) {
        return eventStore.appendMessage(message).map(result -> message);
    }

    private Observable<Void> onUpdateRequested(TilesRendered event, String revision) {
        return updateDesign(event, revision).flatMap(this::sendUpdateEvents);
    }

    private Observable<Design> updateDesign(TilesRendered event, String revision) {
        return eventStore.projectDesign(event.getDesignId(), revision)
                .flatMapObservable(result -> result.map(Observable::just).orElseGet(Observable::empty))
                .flatMapSingle(eventStore::updateDesign)
                .flatMap(result -> result.map(Observable::just).orElseGet(Observable::empty));
    }

    private Observable<Void> sendUpdateEvents(Design design) {
        return createUpdateEvents(design)
                .map(this::createMessage)
                .flatMapSingle(emitter::send);
    }

    private OutputMessage<DesignAggregateUpdated> createMessage(DesignAggregateUpdated event) {
        return MessageFactory.<DesignAggregateUpdated>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }

    private Observable<DesignAggregateUpdated> createUpdateEvents(Design design) {
        return Observable.just(createUpdateEvent(design));
    }

    private DesignAggregateUpdated createUpdateEvent(Design design) {
        return DesignAggregateUpdated.newBuilder()
                .setDesignId(design.getDesignId())
                .setCommandId(design.getCommandId())
                .setUserId(design.getUserId())
                .setRevision(design.getRevision())
                .setChecksum(design.getChecksum())
                .setData(design.getData())
                .setStatus(DesignAggregateStatus.valueOf(design.getStatus()))
                .setPublished(design.isPublished())
                .setLevels(design.getLevels())
                .setBitmap(design.getBitmap())
                .setCreated(design.getCreated().toInstant(ZoneOffset.UTC))
                .setUpdated(design.getUpdated().toInstant(ZoneOffset.UTC))
                .build();
    }
}
