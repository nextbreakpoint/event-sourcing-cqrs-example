package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.AggregateUpdateCompleted;
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
    private final Mapper<Message, AggregateUpdateCompleted> inputMapper;
    private final Mapper<TileRenderRequested, Message> outputMapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public DesignAggregateUpdateCompletedController(Store store, Mapper<Message, AggregateUpdateCompleted> inputMapper, Mapper<TileRenderRequested, Message> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onEventReceived)
                .map(outputMapper::transform)
                .flatMapSingle(emitter::onNext)
                .reduce((r1, r2) -> null)
                .toSingle();
    }

    private Observable<TileRenderRequested> onEventReceived(AggregateUpdateCompleted event) {
        return generateEvents(event, (short)0)
                .concatWith(generateEvents(event, (short)1))
                .concatWith(generateEvents(event, (short)2));
    }

    private Observable<TileRenderRequested> generateEvents(AggregateUpdateCompleted event, short level) {
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
                .flatMap(col ->
                        IntStream.range(0, size)
                                .boxed()
                                .map(row -> new Tile(level, row, col))
                )
                .collect(Collectors.toList());
    }
}
