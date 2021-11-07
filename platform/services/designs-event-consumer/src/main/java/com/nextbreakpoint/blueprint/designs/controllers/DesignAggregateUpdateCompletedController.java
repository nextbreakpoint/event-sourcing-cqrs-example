package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Tile;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignAggregateUpdateCompletedController implements Controller<Message, Void> {
    private final Mapper<Message, DesignAggregateUpdateCompleted> inputMapper;
    private final Mapper<TileRenderRequested, Message> outputMapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public DesignAggregateUpdateCompletedController(Store store, Mapper<Message, DesignAggregateUpdateCompleted> inputMapper, Mapper<TileRenderRequested, Message> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onAggregateUpdateCompleted)
                .map(outputMapper::transform)
                .flatMapSingle(emitter::onNext)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<TileRenderRequested> onAggregateUpdateCompleted(DesignAggregateUpdateCompleted event) {
        return "DELETED".equalsIgnoreCase(event.getStatus()) ? Observable.empty() : generateEvents(event);
    }

    private Observable<TileRenderRequested> generateEvents(DesignAggregateUpdateCompleted event) {
        return generateEvents(event, 0)
                .concatWith(generateEvents(event, 1))
                .concatWith(generateEvents(event, 2))
                .concatWith(generateEvents(event, 3))
                .concatWith(generateEvents(event, 4))
                .concatWith(generateEvents(event, 5))
                .concatWith(generateEvents(event, 6))
                .concatWith(generateEvents(event, 7));
    }

    private Observable<TileRenderRequested> generateEvents(DesignAggregateUpdateCompleted event, int level) {
        return Observable.from(generateTiles(level))
                .map(tile -> new TileRenderRequested(
                        event.getUuid(),
                        event.getEvid(),
                        event.getData(),
                        event.getChecksum(),
                        tile.getLevel(),
                        tile.getRow(),
                        tile.getCol()
                ));
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