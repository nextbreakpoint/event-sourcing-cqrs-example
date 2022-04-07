package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdated;
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
    private final MessageMapper<DesignAggregateUpdated, OutputMessage> updateOutputMapper;
    private final MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper;
    private final MessageEmitter eventsEmitter;
    private final MessageEmitter renderEmitter;
    private final DesignAggregate aggregate;

    public TilesRenderedController(
            DesignAggregate aggregate,
            Mapper<InputMessage, TilesRendered> inputMapper,
            MessageMapper<DesignAggregateUpdated, OutputMessage> updateOutputMapper,
            MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper,
            MessageEmitter eventsEmitter,
            MessageEmitter renderEmitter
    ) {
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
                .flatMapObservable(event -> onUpdateRequested(event, message.getToken()))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(value -> null);
    }

    private Single<InputMessage> onMessageReceived(InputMessage message) {
        return aggregate.appendMessage(message).map(result -> message);
    }

    private Observable<Void> onUpdateRequested(TilesRendered event, String revision) {
        return updateDesign(event, revision).flatMap(design -> sendEvents(event, design, revision));
    }

    private Observable<Design> updateDesign(TilesRendered event, String revision) {
        return aggregate.projectDesign(event.getDesignId(), revision)
                .flatMapObservable(result -> result.map(Observable::just).orElseGet(Observable::empty))
                .flatMapSingle(aggregate::updateDesign)
                .flatMap(result -> result.map(Observable::just).orElseGet(Observable::empty));
    }

    private Observable<? extends Void> sendEvents(TilesRendered event, Design design, String revision) {
        return sendUpdateEvents(design).concatWith(sendRenderEvents(event, design, revision));
    }

    private Observable<Void> sendUpdateEvents(Design design) {
        return createUpdateEvents(design)
                .map(updateOutputMapper::transform)
                .flatMapSingle(eventsEmitter::send);
    }

    private Observable<DesignAggregateUpdated> createUpdateEvents(Design design) {
        return Observable.just(createUpdateEvent(design));
    }

    private DesignAggregateUpdated createUpdateEvent(Design design) {
        return DesignAggregateUpdated.builder()
                .withDesignId(design.getDesignId())
                .withCommandId(design.getCommandId())
                .withUserId(design.getUserId())
                .withRevision(design.getRevision())
                .withChecksum(design.getChecksum())
                .withData(design.getData())
                .withStatus(design.getStatus())
                .withPublished(design.isPublished())
                .withLevels(design.getLevels())
                .withBitmap(design.getBitmap())
                .withCreated(design.getCreated())
                .withUpdated(design.getUpdated())
                .build();
    }

    private Observable<Void> sendRenderEvents(TilesRendered event, Design design, String revision) {
        return createRenderEvents(event, design, revision).flatMapSingle(this::sendRenderEvent);
    }

    private Single<Void> sendRenderEvent(TileRenderRequested event) {
        return renderEmitter.send(renderOutputMapper.transform(event), Render.getTopicName(renderEmitter.getTopicName() + "-requested", event.getLevel()));
    }

    private Observable<TileRenderRequested> createRenderEvents(TilesRendered event, Design design, String revision) {
        return design.getCommandId().equals(event.getCommandId()) ? generateRenderEvents(event, design, revision) : Observable.empty();
    }

    private Observable<TileRenderRequested> generateRenderEvents(TilesRendered event, Design design, String revision) {
        return Observable.from(event.getTiles())
                .flatMap(tile -> Observable.from(generateTiles(tile, design.getLevels())))
                .map(tile -> createRenderEvent(design, tile, revision));
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

    private List<Tile> generateTiles(Tile tile, int levels) {
        int level = tile.getLevel() + 1;
        int row = tile.getRow() * 2;
        int col = tile.getCol() * 2;

        if (tile.getLevel() > 1 && level < 8 && level < levels) {
            final Tile tile0 = new Tile(level, row + 0, col + 0);
            final Tile tile1 = new Tile(level, row + 0, col + 1);
            final Tile tile2 = new Tile(level, row + 1, col + 0);
            final Tile tile3 = new Tile(level, row + 1, col + 1);

            return List.of(tile0, tile1, tile2, tile3);
        } else {
            return List.of();
        }
    }
}
