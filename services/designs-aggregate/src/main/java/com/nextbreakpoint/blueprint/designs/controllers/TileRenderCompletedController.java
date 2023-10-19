package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.model.Design;
import lombok.extern.log4j.Log4j2;
import rx.Observable;
import rx.Single;

import java.util.Objects;
import java.util.UUID;

@Log4j2
public class TileRenderCompletedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileRenderCompleted> inputMapper;
    private final MessageMapper<TileRenderCompleted, OutputMessage> bufferOutputMapper;
    private final MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper;
    private final MessageEmitter bufferEmitter;
    private final MessageEmitter renderEmitter;
    private final DesignAggregate aggregate;

    public TileRenderCompletedController(
            DesignAggregate aggregate,
            Mapper<InputMessage, TileRenderCompleted> inputMapper,
            MessageMapper<TileRenderCompleted, OutputMessage> bufferOutputMapper,
            MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper,
            MessageEmitter bufferEmitter,
            MessageEmitter renderEmitter
    ) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.bufferOutputMapper = Objects.requireNonNull(bufferOutputMapper);
        this.renderOutputMapper = Objects.requireNonNull(renderOutputMapper);
        this.bufferEmitter = Objects.requireNonNull(bufferEmitter);
        this.renderEmitter = Objects.requireNonNull(renderEmitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
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
        return aggregate.findDesign(designId)
                .flatMapObservable(result -> result.map(Observable::just).orElseGet(Observable::empty));
    }

    private Observable<? extends Void> sendEvents(TileRenderCompleted event, Design design, String revision) {
        return sendTileCompletedEvent(event).concatWith(sendRenderEvents(event, design, revision));
    }

    private Observable<Void> sendTileCompletedEvent(TileRenderCompleted event) {
        return Observable.just(event)
                .map(bufferOutputMapper::transform)
                .flatMapSingle(bufferEmitter::send);
    }

    private Observable<Void> sendRenderEvents(TileRenderCompleted event, Design design, String revision) {
        return createRenderEvents(event, design, revision).flatMapSingle(this::sendRenderEvent);
    }

    private Single<Void> sendRenderEvent(TileRenderRequested event) {
        return renderEmitter.send(renderOutputMapper.transform(event), Render.getTopicName(renderEmitter.getTopicName() + "-requested", event.getLevel()));
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
        final TilesBitmap bitmap = TilesBitmap.of(design.getBitmap());

        return Observable.from(Render.generateTiles(creteTile(event), design.getLevels(), bitmap))
                .map(tile -> createRenderEvent(design, tile, revision));
    }

    private Tile creteTile(TileRenderCompleted event) {
        return Tile.builder()
                .withLevel(event.getLevel())
                .withRow(event.getRow())
                .withCol(event.getCol())
                .build();
    }

    private TileRenderRequested createRenderEvent(Design design, Tile tile, String revision) {
        return TileRenderRequested.builder()
                .withDesignId(design.getDesignId())
                .withCommandId(design.getCommandId())
                .withRevision(revision)
                .withData(design.getData())
                .withChecksum(design.getChecksum())
                .withLevel(tile.getLevel())
                .withRow(tile.getRow())
                .withCol(tile.getCol())
                .build();
    }
}
