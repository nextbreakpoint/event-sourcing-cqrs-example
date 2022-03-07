package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.common.core.Tile;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignAggregateUpdateCompletedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAggregateUpdateCompleted> inputMapper;
    private final MessageMapper<TilesRenderRequired, OutputMessage> updateOutputMapper;
    private final MessageMapper<DesignDocumentDeleteRequested, OutputMessage> deleteOutputMapper;
    private final KafkaEmitter updateEmitter;
    private final KafkaEmitter deleteEmitter;

    public DesignAggregateUpdateCompletedController(Mapper<InputMessage, DesignAggregateUpdateCompleted> inputMapper, MessageMapper<TilesRenderRequired, OutputMessage> updateOutputMapper, MessageMapper<DesignDocumentDeleteRequested, OutputMessage> deleteOutputMapper, KafkaEmitter updateEmitter, KafkaEmitter deleteEmitter) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.updateOutputMapper = Objects.requireNonNull(updateOutputMapper);
        this.deleteOutputMapper = Objects.requireNonNull(deleteOutputMapper);
        this.updateEmitter = Objects.requireNonNull(updateEmitter);
        this.deleteEmitter = Objects.requireNonNull(deleteEmitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(event -> onAggregateUpdateCompleted(event, message.getTrace()))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<Void> onAggregateUpdateCompleted(DesignAggregateUpdateCompleted event, Tracing tracing) {
        return "DELETED".equalsIgnoreCase(event.getStatus()) ? onDelete(event, tracing) : onUpdate(event, tracing);
    }

    private Observable<Void> onDelete(DesignAggregateUpdateCompleted event, Tracing tracing) {
        return generateEvents(event)
                .map(deleteEvent -> deleteOutputMapper.transform(Tracing.from(tracing), deleteEvent))
                .flatMapSingle(deleteEmitter::send);
    }

    private Observable<Void> onUpdate(DesignAggregateUpdateCompleted event, Tracing tracing) {
        return generateTiles(event.getLevels())
                .buffer(50)
                .map(tiles -> createEvent(event, tiles))
                .map(renderEvent -> updateOutputMapper.transform(Tracing.from(tracing), renderEvent))
                .flatMapSingle(updateEmitter::send);
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

    private TilesRenderRequired createEvent(DesignAggregateUpdateCompleted event, List<Tile> tiles) {
        return TilesRenderRequired.builder()
                .withDesignId(event.getDesignId())
                .withRevision(event.getRevision())
                .withData(event.getData())
                .withChecksum(event.getChecksum())
                .withTiles(tiles)
                .build();
    }

    private Observable<Tile> generateTiles(int maxLevel) {
        return generateTiles(maxLevel, 0)
                .concatWith(generateTiles(maxLevel, 1))
                .concatWith(generateTiles(maxLevel, 2))
                .concatWith(generateTiles(maxLevel, 3))
                .concatWith(generateTiles(maxLevel, 4))
                .concatWith(generateTiles(maxLevel, 5))
                .concatWith(generateTiles(maxLevel, 6))
                .concatWith(generateTiles(maxLevel, 7));
    }

    private Observable<Tile> generateTiles(int maxLevel, int level) {
        return maxLevel > level ? Observable.from(makeTiles(level)) : Observable.empty();
    }

    private List<Tile> makeTiles(int level) {
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
