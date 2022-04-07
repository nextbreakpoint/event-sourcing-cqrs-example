package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateTilesUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.TilesRendered;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;

public class TilesRenderedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TilesRendered> inputMapper;
    private final MessageMapper<DesignAggregateTilesUpdateRequested, OutputMessage> updateOutputMapper;
    private final MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper;
    private final MessageEmitter eventsEmitter;
    private final MessageEmitter renderEmitter;
    private final DesignAggregate aggregate;

    public TilesRenderedController(DesignAggregate aggregate, Mapper<InputMessage, TilesRendered> inputMapper, MessageMapper<DesignAggregateTilesUpdateRequested, OutputMessage> updateOutputMapper, MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper, MessageEmitter eventsEmitter, MessageEmitter renderEmitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.updateOutputMapper = Objects.requireNonNull(updateOutputMapper);
        this.renderOutputMapper = Objects.requireNonNull(renderOutputMapper);
        this.eventsEmitter = Objects.requireNonNull(eventsEmitter);
        this.renderEmitter = Objects.requireNonNull(renderEmitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .map(inputMapper::transform)
                .flatMapObservable(event -> createEvents(event, message.getToken()))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(value -> null);
    }

    private Single<InputMessage> onMessageReceived(InputMessage message) {
        return aggregate.appendMessage(message).map(result -> message);
    }

    private Observable<Void> createEvents(TilesRendered event, String revision) {
        return aggregate.findDesign(event.getDesignId())
                .flatMapObservable(result -> result.map(design -> createEvents(event, design, revision)).orElseGet(Observable::empty));
    }

    private Observable<Void> createEvents(TilesRendered event, Design design, String revision) {
        return (design.getCommandId().equals(event.getCommandId()) && design.getChecksum().equals(event.getChecksum())) ? generateEvents(event, design, revision) : Observable.empty();
    }

    private Observable<Void> generateEvents(TilesRendered event, Design design, String revision) {
        return generateRenderEvents(event, design, revision).concatWith(generateAggregateEvent(design, revision));
    }

    private Observable<Void> generateRenderEvents(TilesRendered event, Design design, String revision) {
        return Observable.from(event.getTiles())
                .flatMap(tile -> Observable.from(generateTiles(tile, design.getLevels(), TilesBitmap.of(design.getBitmap()))))
                .flatMap(tile -> generateRenderEvent(design, tile, revision));
    }

    private Observable<Void> generateRenderEvent(Design design, Tile tile, String revision) {
        return Observable.just(createRenderEvent(design, tile, revision))
                .map(renderOutputMapper::transform)
                .flatMapSingle(message -> renderEmitter.send(message, Render.getTopicName(renderEmitter.getTopicName() + "-requested", tile.getLevel())));
    }

    private Observable<Void> generateAggregateEvent(Design design, String revision) {
        return Observable.just(createAggregateEvent(design, revision))
                .map(updateOutputMapper::transform)
                .flatMapSingle(eventsEmitter::send);
    }

    private TileRenderRequested createRenderEvent(Design design, Tile tile, String revision) {
        return TileRenderRequested.builder()
                .withDesignId(design.getDesignId())
                .withCommandId(design.getCommandId())
                .withRevision(revision)
                .withData(design.getData())
                .withChecksum(design.getChecksum())
                .withLevel(tile.getLevel())
                .withRow(tile.getRow())
                .withCol(tile.getCol())
                .build();
    }

    private DesignAggregateTilesUpdateRequested createAggregateEvent(Design design, String revision) {
        return DesignAggregateTilesUpdateRequested.builder()
                .withDesignId(design.getDesignId())
                .withCommandId(design.getCommandId())
                .withRevision(revision)
                .build();
    }

    private List<Tile> generateTiles(Tile tile, int levels, TilesBitmap bitmap) {
        int level = tile.getLevel() + 1;
        int row = tile.getRow() * 2;
        int col = tile.getCol() * 2;

        if (tile.getLevel() > 1 && level < 8 && level < levels) {
            final Tile tile0 = new Tile(level, row + 0, col + 0);
            final Tile tile1 = new Tile(level, row + 0, col + 1);
            final Tile tile2 = new Tile(level, row + 1, col + 0);
            final Tile tile3 = new Tile(level, row + 1, col + 1);

//            if (level < 7) {
                return List.of(tile0, tile1, tile2, tile3);
//            } else {
//                List<Tile> tiles = new ArrayList<>();
//
//                if (!bitmap.hasTile(level, tile0.getRow(), tile0.getCol())) {
//                    tiles.add(tile0);
//                }
//
//                if (!bitmap.hasTile(level, tile1.getRow(), tile1.getCol())) {
//                    tiles.add(tile1);
//                }
//
//                if (!bitmap.hasTile(level, tile2.getRow(), tile2.getCol())) {
//                    tiles.add(tile2);
//                }
//
//                if (!bitmap.hasTile(level, tile3.getRow(), tile3.getCol())) {
//                    tiles.add(tile3);
//                }
//
//                return tiles;
//            }
        } else {
            return List.of();
        }
    }
}
