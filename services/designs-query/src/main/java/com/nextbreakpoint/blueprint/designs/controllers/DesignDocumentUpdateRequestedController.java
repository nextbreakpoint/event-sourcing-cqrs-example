package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Time;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.Tiles;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.LevelTiles;
import com.nextbreakpoint.blueprint.designs.persistence.dto.DeleteDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.InsertDesignRequest;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;

public class DesignDocumentUpdateRequestedController implements Controller<InputMessage<DesignDocumentUpdateRequested>, Void> {
    private final String messageSource;
    private final Store store;
    private final MessageEmitter<DesignDocumentUpdateCompleted> emitter;

    public DesignDocumentUpdateRequestedController(String messageSource, Store store, MessageEmitter<DesignDocumentUpdateCompleted> emitter) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.store = Objects.requireNonNull(store);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage<DesignDocumentUpdateRequested> message) {
        return Single.fromCallable(() -> message.getValue().getData())
                .flatMapObservable(this::onDesignDocumentUpdateRequested)
                .map(this::createMessage)
                .flatMapSingle(emitter::send)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<DesignDocumentUpdateCompleted> onDesignDocumentUpdateRequested(DesignDocumentUpdateRequested event) {
        return store.insertDesign(new InsertDesignRequest(event.getDesignId(), createDesign(event), true))
                .flatMap(ignored -> updateOrDelete(event))
                .map(ignored -> new DesignDocumentUpdateCompleted(event.getDesignId(), event.getCommandId(), event.getRevision()))
                .toObservable();
    }

    private OutputMessage<DesignDocumentUpdateCompleted> createMessage(DesignDocumentUpdateCompleted event) {
        return MessageFactory.<DesignDocumentUpdateCompleted>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }

    private Single<?> updateOrDelete(DesignDocumentUpdateRequested event) {
        if (isCompletedAndPublished(event)) {
            return store.insertDesign(new InsertDesignRequest(event.getDesignId(), createDesign(event), false));
        } else {
            return store.deleteDesign(new DeleteDesignRequest(event.getDesignId(), false));
        }
    }

    private boolean isCompletedAndPublished(DesignDocumentUpdateRequested event) {
        return event.getLevels() == 8 && completedTiles(event.getTiles()) == 21845 && event.getPublished();
    }

    private int completedTiles(List<Tiles> tiles) {
        return tiles.stream().mapToInt(Tiles::getCompleted).sum();
    }

    private Design createDesign(DesignDocumentUpdateRequested event) {
        return Design.builder()
                .withDesignId(event.getDesignId())
                .withUserId(event.getUserId())
                .withCommandId(event.getCommandId())
                .withChecksum(event.getChecksum())
                .withRevision(event.getRevision())
                .withData(event.getData())
                .withStatus(event.getStatus().name())
                .withPublished(event.getPublished())
                .withLevels(event.getLevels())
                .withTiles(getTiles(event))
                .withCreated(Time.format(event.getCreated()))
                .withUpdated(Time.format(event.getUpdated()))
                .build();
    }

    private static List<LevelTiles> getTiles(DesignDocumentUpdateRequested event) {
        return event.getTiles().stream().map(DesignDocumentUpdateRequestedController::getTiles).toList();
    }

    private static LevelTiles getTiles(Tiles tile) {
        return LevelTiles.builder()
                .withLevel(tile.getLevel())
                .withTotal(tile.getTotal())
                .withCompleted(tile.getCompleted())
                .build();
    }
}
