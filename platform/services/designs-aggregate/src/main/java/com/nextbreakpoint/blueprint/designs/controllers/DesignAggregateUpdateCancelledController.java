package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCancelled;
import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignAggregateUpdateCancelledController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAggregateUpdateCancelled> inputMapper;
    private final MessageMapper<TileRenderAborted, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final DesignAggregate aggregate;

    public DesignAggregateUpdateCancelledController(DesignAggregate aggregate, Mapper<InputMessage, DesignAggregateUpdateCancelled> inputMapper, MessageMapper<TileRenderAborted, OutputMessage> outputMapper, MessageEmitter emitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
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
                .flatMapSingle(emitter::send)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<TileRenderAborted> onTileRenderAborted(DesignAggregateUpdateCancelled event) {
        return aggregate.findDesign(event.getDesignId())
                .map(result -> result.orElseThrow(() -> new RuntimeException("Design not found " + event.getDesignId())))
                .flatMapObservable(design -> generateEvents(event, design));
    }

    private Observable<TileRenderAborted> generateEvents(DesignAggregateUpdateCancelled event, Design design) {
        return generateEvents(event, design, 0)
                .concatWith(generateEvents(event, design, 1))
                .concatWith(generateEvents(event, design, 2))
                .concatWith(generateEvents(event, design, 3))
                .concatWith(generateEvents(event, design, 4))
                .concatWith(generateEvents(event, design, 5))
                .concatWith(generateEvents(event, design, 6))
                .concatWith(generateEvents(event, design, 7));
    }

    private Observable<TileRenderAborted> generateEvents(DesignAggregateUpdateCancelled event, Design design, int level) {
        if (design.getLevels() > level) {
            return Observable.from(generateTiles(level)).map(tile -> createEvent(event, design, tile));
        } else {
            return Observable.empty();
        }
    }

    private TileRenderAborted createEvent(DesignAggregateUpdateCancelled event, Design design, Tile tile) {
        return TileRenderAborted.builder()
                .withDesignId(event.getDesignId())
                .withRevision(design.getRevision())
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
