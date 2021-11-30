package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignAbortRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.Tile;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignAbortRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAbortRequested> inputMapper;
    private final Mapper<TileRenderAborted, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private Store store;

    public DesignAbortRequestedController(Store store, Mapper<InputMessage, DesignAbortRequested> inputMapper, Mapper<TileRenderAborted, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onTileRenderAborted)
                .map(outputMapper::transform)
                .flatMapSingle(emitter::onNext)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<TileRenderAborted> onTileRenderAborted(DesignAbortRequested event) {
        return store.findDesign(event.getUuid())
                .map(result -> result.orElseThrow(() -> new RuntimeException("Design not found " + event.getUuid())))
                .flatMapObservable(design -> generateEvents(event, design));
    }

    private Observable<TileRenderAborted> generateEvents(DesignAbortRequested event, Design design) {
        return generateEvents(event, design, 0)
                .concatWith(generateEvents(event, design, 1))
                .concatWith(generateEvents(event, design, 2))
                .concatWith(generateEvents(event, design, 3))
                .concatWith(generateEvents(event, design, 4))
                .concatWith(generateEvents(event, design, 5))
                .concatWith(generateEvents(event, design, 6))
                .concatWith(generateEvents(event, design, 7));
    }

    private Observable<TileRenderAborted> generateEvents(DesignAbortRequested event, Design design, int level) {
        if (design.getLevels() > level) {
            return Observable.from(generateTiles(level)).map(tile -> createEvent(event, design, tile));
        } else {
            return Observable.empty();
        }
    }

    private TileRenderAborted createEvent(DesignAbortRequested event, Design design, Tile tile) {
        return TileRenderAborted.builder()
                .withEvid(Uuids.timeBased())
                .withUuid(event.getUuid())
                .withEsid(design.getEsid())
                .withChecksum(event.getChecksum())
                .withLevel(tile.getLevel())
                .withRow(tile.getRow())
                .withCol(tile.getCol())
                .build();
    }

    private List<Tile> generateTiles(int level) {
        final int size = (int) Math.rint(Math.pow(2, level));
        return IntStream.range(0, size)
                .boxed()
                .flatMap(row ->
                        IntStream.range(0, size)
                                .boxed()
                                .map(col -> new Tile(level, row, col))
                )
                .collect(Collectors.toList());
    }
}
