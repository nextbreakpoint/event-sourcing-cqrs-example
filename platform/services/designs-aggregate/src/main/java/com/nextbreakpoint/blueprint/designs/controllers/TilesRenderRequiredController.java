package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.TilesRenderRequired;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import rx.Observable;
import rx.Single;

import java.util.Objects;

public class TilesRenderRequiredController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TilesRenderRequired> inputMapper;
    private final MessageMapper<TileRenderRequested, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;

    public TilesRenderRequiredController(Mapper<InputMessage, TilesRenderRequired> inputMapper, MessageMapper<TileRenderRequested, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onTilesRenderRequiredCompleted)
                .map(event -> outputMapper.transform(Tracing.from(message.getTrace()), event))
                .flatMapSingle(emitter::onNext)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<TileRenderRequested> onTilesRenderRequiredCompleted(TilesRenderRequired event) {
        return Observable.from(event.getTiles()).map(tile -> createEvent(event, tile));
    }

    private TileRenderRequested createEvent(TilesRenderRequired event, Tile tile) {
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
}
