package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class DesignUpdateController<T> implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, T> inputMapper;
    private final MessageMapper<DesignAggregateUpdated, OutputMessage> updateOutputMapper;
    private final MessageMapper<TileRenderCancelled, OutputMessage> cancelOutputMapper;
    private final MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper;
    private final MessageEmitter updateEmitter;
    private final MessageEmitter cancelEmitter;
    private final MessageEmitter renderEmitter;
    private final DesignAggregate aggregate;
    private final Function<T, UUID> extractor;

    public DesignUpdateController(
            DesignAggregate aggregate,
            Mapper<InputMessage, T> inputMapper,
            MessageMapper<DesignAggregateUpdated, OutputMessage> updateOutputMapper,
            MessageMapper<TileRenderCancelled, OutputMessage> cancelOutputMapper,
            MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper,
            MessageEmitter updateEmitter,
            MessageEmitter cancelEmitter,
            MessageEmitter renderEmitter,
            Function<T, UUID> extractor
    ) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.updateOutputMapper = Objects.requireNonNull(updateOutputMapper);
        this.cancelOutputMapper = Objects.requireNonNull(cancelOutputMapper);
        this.renderOutputMapper = Objects.requireNonNull(renderOutputMapper);
        this.updateEmitter = Objects.requireNonNull(updateEmitter);
        this.cancelEmitter = Objects.requireNonNull(cancelEmitter);
        this.renderEmitter = Objects.requireNonNull(renderEmitter);
        this.extractor = Objects.requireNonNull(extractor);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .map(inputMapper::transform)
                .flatMapObservable(event -> onEventReceived(extractor.apply(event), message.getToken()))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(value -> null);
    }

    private Single<InputMessage> onMessageReceived(InputMessage message) {
        return aggregate.appendMessage(message).map(result -> message);
    }

    private Observable<Void> onEventReceived(UUID designId, String revision) {
        return updateDesign(designId, revision).flatMap(this::sendEvents);
//        return cancelDesign(designId).concatWith(updateDesign(designId, revision).flatMap(this::sendEvents));
    }

    private Observable<Void> cancelDesign(UUID designId) {
        return aggregate.findDesign(designId)
                .flatMapObservable(result -> result.map(this::cancelDesign).orElseGet(Observable::empty));
    }

    private Observable<Void> cancelDesign(Design design) {
        return createCancelEvents(design)
                .map(cancelOutputMapper::transform)
                .flatMapSingle(cancelEmitter::send);
    }

    private Observable<Design> updateDesign(UUID designId, String revision) {
        return aggregate.projectDesign(designId, revision)
                .flatMapObservable(result -> result.map(Observable::just).orElseGet(Observable::empty))
                .flatMapSingle(aggregate::updateDesign)
                .flatMap(result -> result.map(Observable::just).orElseGet(Observable::empty));
    }

    private Observable<Void> sendEvents(Design design) {
        return sendUpdateEvents(design).concatWith(sendRenderEvents(design));
    }

    private Observable<Void> sendUpdateEvents(Design design) {
        return createUpdateEvents(design)
                .map(updateOutputMapper::transform)
                .flatMapSingle(updateEmitter::send);
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

    private Observable<Void> sendRenderEvents(Design design) {
        if (design.getStatus().equals("DELETED")) {
            return Observable.empty();
        } else {
            return createRenderEvents(design).flatMapSingle(this::sendRenderEvent);
        }
    }

    private Single<Void> sendRenderEvent(TileRenderRequested event) {
        return renderEmitter.send(renderOutputMapper.transform(event), Render.getTopicName(renderEmitter.getTopicName() + "-requested", 0));
    }

    private Observable<TileRenderRequested> createRenderEvents(Design design) {
        return generateTiles(0)
                .concatWith(generateTiles(1))
                .concatWith(generateTiles(2))
                .map(tile -> createRenderEvent(design, tile));
    }

    private TileRenderRequested createRenderEvent(Design design, Tile tile) {
        return TileRenderRequested.builder()
                .withDesignId(design.getDesignId())
                .withCommandId(design.getCommandId())
                .withRevision(design.getRevision())
                .withChecksum(design.getChecksum())
                .withData(design.getData())
                .withLevel(tile.getLevel())
                .withRow(tile.getRow())
                .withCol(tile.getCol())
                .build();
    }

    private Observable<TileRenderCancelled> createCancelEvents(Design design) {
        return generateTiles(0)
                .concatWith(generateTiles(1))
                .concatWith(generateTiles(2))
                .map(tile -> createCancelEvent(design, tile));
    }

    private TileRenderCancelled createCancelEvent(Design design, Tile tile) {
        return TileRenderCancelled.builder()
                .withDesignId(design.getDesignId())
                .withCommandId(design.getCommandId())
                .withRevision(design.getRevision())
                .withChecksum(design.getChecksum())
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

    public static class DesignInsertRequestedController extends DesignUpdateController<DesignInsertRequested> {
        public DesignInsertRequestedController(
                DesignAggregate aggregate,
                Mapper<InputMessage, DesignInsertRequested> inputMapper,
                MessageMapper<DesignAggregateUpdated, OutputMessage> updateOutputMapper,
                MessageMapper<TileRenderCancelled, OutputMessage> cancelOutputMapper,
                MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper,
                MessageEmitter eventsEmitter,
                MessageEmitter cancelEmitter,
                MessageEmitter renderEmitter
        ) {
            super(aggregate, inputMapper, updateOutputMapper, cancelOutputMapper, renderOutputMapper, eventsEmitter, cancelEmitter, renderEmitter, DesignInsertRequested::getDesignId);
        }
    }

    public static class DesignUpdateRequestedController extends DesignUpdateController<DesignUpdateRequested> {
        public DesignUpdateRequestedController(
                DesignAggregate aggregate,
                Mapper<InputMessage, DesignUpdateRequested> inputMapper,
                MessageMapper<DesignAggregateUpdated, OutputMessage> updateOutputMapper,
                MessageMapper<TileRenderCancelled, OutputMessage> cancelOutputMapper,
                MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper,
                MessageEmitter eventsEmitter,
                MessageEmitter cancelEmitter,
                MessageEmitter renderEmitter
        ) {
            super(aggregate, inputMapper, updateOutputMapper, cancelOutputMapper, renderOutputMapper, eventsEmitter, cancelEmitter, renderEmitter, DesignUpdateRequested::getDesignId);
        }
    }

    public static class DesignDeleteRequestedController extends DesignUpdateController<DesignDeleteRequested> {
        public DesignDeleteRequestedController(
                DesignAggregate aggregate,
                Mapper<InputMessage, DesignDeleteRequested> inputMapper,
                MessageMapper<DesignAggregateUpdated, OutputMessage> updateOutputMapper,
                MessageMapper<TileRenderCancelled, OutputMessage> cancelOutputMapper,
                MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper,
                MessageEmitter eventsEmitter,
                MessageEmitter cancelEmitter,
                MessageEmitter renderEmitter
        ) {
            super(aggregate, inputMapper, updateOutputMapper, cancelOutputMapper, renderOutputMapper, eventsEmitter, cancelEmitter, renderEmitter, DesignDeleteRequested::getDesignId);
        }
    }
}
