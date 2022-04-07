package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.common.Render;
import rx.Observable;
import rx.Single;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DesignAggregateUpdateCompletedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAggregateUpdateCompleted> inputMapper;
    private final MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper;
    private final MessageMapper<DesignDocumentDeleteRequested, OutputMessage> deleteOutputMapper;
    private final MessageEmitter eventsEmitter;
    private final MessageEmitter renderEmitter;
    private final DesignAggregate aggregate;

    public DesignAggregateUpdateCompletedController(DesignAggregate aggregate, Mapper<InputMessage, DesignAggregateUpdateCompleted> inputMapper, MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper, MessageMapper<DesignDocumentDeleteRequested, OutputMessage> deleteOutputMapper, MessageEmitter eventsEmitter, MessageEmitter renderEmitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.renderOutputMapper = Objects.requireNonNull(renderOutputMapper);
        this.deleteOutputMapper = Objects.requireNonNull(deleteOutputMapper);
        this.eventsEmitter = Objects.requireNonNull(eventsEmitter);
        this.renderEmitter = Objects.requireNonNull(renderEmitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onAggregateUpdateCompleted)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<Void> onAggregateUpdateCompleted(DesignAggregateUpdateCompleted event) {
        return "DELETED".equalsIgnoreCase(event.getStatus()) ? onDelete(event) : onUpdate(event);
    }

    private Observable<Void> onDelete(DesignAggregateUpdateCompleted event) {
        return Observable.just(createDeleteEvent(event))
                .map(deleteOutputMapper::transform)
                .flatMapSingle(eventsEmitter::send);
    }

    private Observable<Void> onUpdate(DesignAggregateUpdateCompleted event) {
        return createRenderEvents(event)
                .map(renderOutputMapper::transform)
                .flatMapSingle(message -> renderEmitter.send(message, Render.getTopicName(renderEmitter.getTopicName() + "-requested", 0)));
    }

    private DesignDocumentDeleteRequested createDeleteEvent(DesignAggregateUpdateCompleted event) {
        return DesignDocumentDeleteRequested.builder()
                .withDesignId(event.getDesignId())
                .withCommandId(event.getCommandId())
                .withRevision(event.getRevision())
                .build();
    }

    private Observable<TileRenderRequested> createRenderEvents(DesignAggregateUpdateCompleted event) {
        return generateTiles(0)
                .concatWith(generateTiles(1))
                .concatWith(generateTiles(2))
                .map(tile -> createRenderEvent(event, tile));
    }

    private TileRenderRequested createRenderEvent(DesignAggregateUpdateCompleted event, Tile tile) {
        return TileRenderRequested.builder()
                .withDesignId(event.getDesignId())
                .withCommandId(event.getCommandId())
                .withRevision(event.getRevision())
                .withChecksum(event.getChecksum())
                .withData(event.getData())
                .withLevel(tile.getLevel())
                .withRow(tile.getRow())
                .withCol(tile.getCol())
                .build();
    }

    private Observable<Tile> generateTiles(int level) {
        return Observable.from(makeTiles(level).collect(Collectors.toList()));
    }

    private Stream<Tile> makeTiles(int level) {
        return makeAll(level, (int) Math.rint(Math.pow(2, level)));
    }

    private Stream<Tile> makeAll(int level, int size) {
        return IntStream.range(0, size)
                .boxed()
                .flatMap(row -> makeRow(level, row, size));
    }

    private Stream<Tile> makeRow(int level, int row, int size) {
        return IntStream.range(0, size)
                .boxed()
                .map(col -> new Tile(level, row, col));
    }
}
