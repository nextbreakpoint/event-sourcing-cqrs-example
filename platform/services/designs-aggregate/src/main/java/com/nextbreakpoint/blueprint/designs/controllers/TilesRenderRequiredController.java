package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.TilesRenderRequired;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.common.vertx.TombstoneEmitter;
import com.nextbreakpoint.blueprint.designs.common.Render;
import rx.Observable;
import rx.Single;

import java.util.Objects;

public class TilesRenderRequiredController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TilesRenderRequired> inputMapper;
    private final MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper;
    private final KafkaEmitter renderEmitter;
    private final TombstoneEmitter tombstoneEmitter;

    public TilesRenderRequiredController(Mapper<InputMessage, TilesRenderRequired> inputMapper, MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper, KafkaEmitter renderEmitter, TombstoneEmitter tombstoneEmitter) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.renderOutputMapper = Objects.requireNonNull(renderOutputMapper);
        this.renderEmitter = Objects.requireNonNull(renderEmitter);
        this.tombstoneEmitter = Objects.requireNonNull(tombstoneEmitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(event -> onTilesRenderRequiredCompleted(event, message.getTrace()))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<Void> onTilesRenderRequiredCompleted(TilesRenderRequired event, Tracing tracing) {
        return createAbortEvents(event, tracing).concatWith(createRenderEvents(event, tracing));
    }

    private Observable<Void> createRenderEvents(TilesRenderRequired event, Tracing tracing) {
        return Observable.from(event.getTiles())
                .map(tile -> createRenderEvent(event, tile))
                .flatMapSingle(renderEvent -> renderEmitter.send(renderOutputMapper.transform(Tracing.from(tracing), renderEvent), makeRenderTopicName(renderEvent)));
    }

    private String makeRenderTopicName(TileRenderRequested renderEvent) {
        return renderEvent.getLevel() < 4 ? renderEmitter.getTopicName() + "-0" : renderEmitter.getTopicName() + "-" + (renderEvent.getLevel() - 3);
    }

    private Observable<Void> createAbortEvents(TilesRenderRequired event, Tracing tracing) {
        return Observable.from(event.getTiles())
                .map(tile -> createAbortEvent(event, tile))
                .map(this::createTombstone)
                .flatMapSingle(tombstoneEmitter::onNext);
    }

    private TileRenderRequested createRenderEvent(TilesRenderRequired event, Tile tile) {
        return TileRenderRequested.builder()
                .withDesignId(event.getDesignId())
                .withRevision(event.getRevision())
                .withData(event.getData())
                .withChecksum(event.getChecksum())
                .withLevel(tile.getLevel())
                .withRow(tile.getRow())
                .withCol(tile.getCol())
                .build();
    }

    private TileRenderAborted createAbortEvent(TilesRenderRequired event, Tile tile) {
        return TileRenderAborted.builder()
                .withDesignId(event.getDesignId())
                .withRevision(event.getRevision())
                .withChecksum(event.getChecksum())
                .withLevel(tile.getLevel())
                .withRow(tile.getRow())
                .withCol(tile.getCol())
                .build();
    }

    private Tombstone createTombstone(TileRenderAborted event) {
        return new Tombstone(Render.createRenderKey(event));
    }
}
