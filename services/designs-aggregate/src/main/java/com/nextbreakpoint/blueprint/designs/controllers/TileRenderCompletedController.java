package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.Tile;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.common.Bitmap;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.model.Design;
import lombok.extern.log4j.Log4j2;
import rx.Observable;
import rx.Single;

import java.util.Objects;
import java.util.UUID;

@Log4j2
public class TileRenderCompletedController implements Controller<InputMessage<TileRenderCompleted>, Void> {
    private final String messageSource;
    private final MessageEmitter<TileRenderCompleted> bufferEmitter;
    private final MessageEmitter<TileRenderRequested> renderEmitter;
    private final DesignEventStore eventStore;

    public TileRenderCompletedController(
            String messageSource,
            DesignEventStore eventStore,
            MessageEmitter<TileRenderCompleted> bufferEmitter,
            MessageEmitter<TileRenderRequested> renderEmitter
    ) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.eventStore = Objects.requireNonNull(eventStore);
        this.bufferEmitter = Objects.requireNonNull(bufferEmitter);
        this.renderEmitter = Objects.requireNonNull(renderEmitter);
    }

    @Override
    public Single<Void> onNext(InputMessage<TileRenderCompleted> message) {
        return Single.fromCallable(() -> message.getValue().getData())
                .flatMapObservable(event -> onUpdateRequested(event, message.getToken()))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(value -> null);
    }

    private Observable<Void> onUpdateRequested(TileRenderCompleted event, String revision) {
        return findDesign(event.getDesignId()).flatMap(design -> sendEvents(event, design, revision));
    }

    private Observable<Design> findDesign(UUID designId) {
        return eventStore.findDesign(designId)
                .flatMapObservable(result -> result.map(Observable::just).orElseGet(Observable::empty));
    }

    private Observable<Void> sendEvents(TileRenderCompleted event, Design design, String revision) {
        return sendTileCompletedEvent(event).concatWith(sendRenderEvents(event, design, revision));
    }

    private Observable<Void> sendTileCompletedEvent(TileRenderCompleted event) {
        return Observable.just(event)
                .map(this::createMessage)
                .flatMapSingle(bufferEmitter::send);
    }

    private Observable<Void> sendRenderEvents(TileRenderCompleted event, Design design, String revision) {
        return createRenderEvents(event, design, revision).flatMapSingle(this::sendRenderEvent);
    }

    private Single<Void> sendRenderEvent(TileRenderRequested event) {
        return Single.just(event)
                .map(this::createMessage)
                .flatMap(message -> renderEmitter.send(message, Render.getTopicName(renderEmitter.getTopicName() + "-requested", event.getLevel())));
    }

    private Observable<TileRenderRequested> createRenderEvents(TileRenderCompleted event, Design design, String revision) {
        return isLateEvent(event, design) ? Observable.empty() : generateRenderEvents(event, design, revision);
    }

    private boolean isLateEvent(TileRenderCompleted event, Design design) {
        final boolean value = !event.getCommandId().equals(design.getCommandId());
        if (value) {
            log.debug("Discard late event {}", event);
        }
        return value;
    }

    private Observable<TileRenderRequested> generateRenderEvents(TileRenderCompleted event, Design design, String revision) {
        final Bitmap bitmap = Bitmap.of(design.getBitmap());

        return Observable.from(Render.generateTiles(createTile(event), design.getLevels(), bitmap))
                .map(tile -> createRenderEvent(design, tile, revision));
    }

    private OutputMessage<TileRenderCompleted> createMessage(TileRenderCompleted event) {
        return MessageFactory.<TileRenderCompleted>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }

    private OutputMessage<TileRenderRequested> createMessage(TileRenderRequested event) {
        return MessageFactory.<TileRenderRequested>of(messageSource)
                .createOutputMessage(Render.createRenderKey(event), event);
    }

    private Tile createTile(TileRenderCompleted event) {
        return Tile.newBuilder()
                .setLevel(event.getLevel())
                .setRow(event.getRow())
                .setCol(event.getCol())
                .build();
    }

    private TileRenderRequested createRenderEvent(Design design, Tile tile, String revision) {
        return TileRenderRequested.newBuilder()
                .setDesignId(design.getDesignId())
                .setCommandId(design.getCommandId())
                .setRevision(revision)
                .setData(design.getData())
                .setChecksum(design.getChecksum())
                .setLevel(tile.getLevel())
                .setRow(tile.getRow())
                .setCol(tile.getCol())
                .build();
    }
}
