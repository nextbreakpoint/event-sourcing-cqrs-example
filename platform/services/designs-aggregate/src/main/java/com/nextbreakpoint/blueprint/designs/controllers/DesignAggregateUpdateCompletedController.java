package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.model.Tile;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignAggregateUpdateCompletedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAggregateUpdateCompleted> inputMapper;
    private final MessageMapper<TileRenderRequested, OutputMessage> updateOutputMapper;
    private final MessageMapper<DesignDocumentDeleteRequested, OutputMessage> deleteOutputMapper;
    private final KafkaEmitter emitter;

    public DesignAggregateUpdateCompletedController(Mapper<InputMessage, DesignAggregateUpdateCompleted> inputMapper, MessageMapper<TileRenderRequested, OutputMessage> updateOutputMapper, MessageMapper<DesignDocumentDeleteRequested, OutputMessage> deleteOutputMapper, KafkaEmitter emitter) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.updateOutputMapper = Objects.requireNonNull(updateOutputMapper);
        this.deleteOutputMapper = Objects.requireNonNull(deleteOutputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(event -> onAggregateUpdateCompleted(event, message.getTrace()))
                .flatMapSingle(emitter::onNext)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<OutputMessage> onAggregateUpdateCompleted(DesignAggregateUpdateCompleted event, Tracing tracing) {
        return "DELETED".equalsIgnoreCase(event.getStatus()) ? onDelete(event, tracing) : onUpdate(event, tracing);
    }

    private Observable<OutputMessage> onDelete(DesignAggregateUpdateCompleted event, Tracing tracing) {
        return generateEvents(event)
                .map(deleteEvent -> deleteOutputMapper.transform(Tracing.from(tracing), deleteEvent));
    }

    private Observable<OutputMessage> onUpdate(DesignAggregateUpdateCompleted event, Tracing tracing) {
        return generateEvents(event, 0)
                .concatWith(generateEvents(event, 1))
                .concatWith(generateEvents(event, 2))
                .concatWith(generateEvents(event, 3))
                .concatWith(generateEvents(event, 4))
                .concatWith(generateEvents(event, 5))
                .concatWith(generateEvents(event, 6))
                .concatWith(generateEvents(event, 7))
                .map(renderEvent -> updateOutputMapper.transform(Tracing.from(tracing), renderEvent));
    }

    private Observable<TileRenderRequested> generateEvents(DesignAggregateUpdateCompleted event, int level) {
        if (event.getLevels() > level) {
            return Observable.from(generateTiles(level)).map(tile -> createEvent(event, tile));
        } else {
            return Observable.empty();
        }
    }

    private Observable<DesignDocumentDeleteRequested> generateEvents(DesignAggregateUpdateCompleted event) {
        return Observable.just(createEvent(event));
    }

    private DesignDocumentDeleteRequested createEvent(DesignAggregateUpdateCompleted event) {
        return DesignDocumentDeleteRequested.builder()
                .withDesignId(event.getDesignId())
                .withRevision(event.getRevision())
                .build();
    }

    private TileRenderRequested createEvent(DesignAggregateUpdateCompleted event, Tile tile) {
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
