package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;

public class TileRenderCompletedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileRenderCompleted> inputMapper;
    private final MessageMapper<TileAggregateUpdateRequired, OutputMessage> updateOutputMapper;
    private final MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper;
    private final MessageEmitter eventsEmitter;
    private final MessageEmitter renderEmitter;
    private final DesignAggregate aggregate;

    public TileRenderCompletedController(DesignAggregate aggregate, Mapper<InputMessage, TileRenderCompleted> inputMapper, MessageMapper<TileAggregateUpdateRequired, OutputMessage> updateOutputMapper, MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper, MessageEmitter eventsEmitter, MessageEmitter renderEmitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.updateOutputMapper = Objects.requireNonNull(updateOutputMapper);
        this.renderOutputMapper = Objects.requireNonNull(renderOutputMapper);
        this.eventsEmitter = Objects.requireNonNull(eventsEmitter);
        this.renderEmitter = Objects.requireNonNull(renderEmitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message).flatMap(this::onMessageReceived);
    }

    private Single<Void> onMessageReceived(InputMessage message) {
        return aggregate.appendMessage(message)
                .map(result -> inputMapper.transform(message))
                .flatMapObservable(event -> createEvents(event, message.getToken()))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(value -> null);
    }

    private Observable<Void> createEvents(TileRenderCompleted event, String revision) {
        return aggregate.findDesign(event.getDesignId())
                .flatMapObservable(result -> result.map(design -> createEvents(event, design, revision)).orElseGet(Observable::empty));
    }

    private Observable<Void> createEvents(TileRenderCompleted event, Design design, String revision) {
        return design.getChecksum().equals(event.getChecksum()) ? generateEvents(event, design, revision) : Observable.empty();
    }

    private Observable<Void> generateEvents(TileRenderCompleted event, Design design, String revision) {
        return generateRenderEvents(event, design, revision).concatWith(generateAggregateEvent(design, revision));
    }

    private Observable<Void> generateRenderEvents(TileRenderCompleted event, Design design, String revision) {
        return Observable.from(generateTiles(event, design.getLevels()))
                .flatMap(tile -> generateRenderEvent(design, tile, revision));
    }

    private Observable<Void> generateRenderEvent(Design design, Tile tile, String revision) {
        return Observable.just(createRenderEvent(design, tile, revision))
                .map(renderOutputMapper::transform)
                .flatMapSingle(message -> renderEmitter.send(message, getTopicName(tile)));
    }

    private Observable<Void> generateAggregateEvent(Design design, String revision) {
        return Observable.just(createAggregateEvent(design, revision))
                .map(updateOutputMapper::transform)
                .flatMapSingle(eventsEmitter::send);
    }

    private TileRenderRequested createRenderEvent(Design design, Tile tile,  String revision) {
        return TileRenderRequested.builder()
                .withDesignId(design.getDesignId())
                .withRevision(revision)
                .withData(design.getData())
                .withChecksum(design.getChecksum())
                .withLevel(tile.getLevel())
                .withCol(tile.getCol())
                .withRow(tile.getRow())
                .build();
    }

    private TileAggregateUpdateRequired createAggregateEvent(Design design, String revision) {
        return TileAggregateUpdateRequired.builder()
                .withDesignId(design.getDesignId())
                .withRevision(revision)
                .build();
    }

    private List<Tile> generateTiles(TileRenderCompleted event, int maxLevel) {
        int level = event.getLevel() + 1;
        int row = event.getRow() * 2;
        int col = event.getCol() * 2;

        if (event.getLevel() > 1 && level < 8 && level < maxLevel) {
            return List.of(
                    new Tile(level, row + 0, col + 0),
                    new Tile(level, row + 0, col + 1),
                    new Tile(level, row + 1, col + 0),
                    new Tile(level, row + 1, col + 1)
            );
        } else {
            return List.of();
        }
    }

    private String getTopicName(Tile tile) {
        if (tile.getLevel() < 4) {
            return renderEmitter.getTopicName() + "-0";
        } else {
            return renderEmitter.getTopicName() + "-" + (tile.getLevel() - 3);
        }
    }
}
