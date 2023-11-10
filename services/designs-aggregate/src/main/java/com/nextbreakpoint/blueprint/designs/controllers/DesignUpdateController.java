package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateStatus;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.Tile;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.model.Design;
import org.apache.avro.specific.SpecificRecord;
import rx.Observable;
import rx.Single;

import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class DesignUpdateController<T extends SpecificRecord> implements Controller<InputMessage<T>, Void> {
    private final MessageEmitter<DesignAggregateUpdated> updateEmitter;
    private final MessageEmitter<TileRenderRequested> renderEmitter;
    private final DesignEventStore eventStore;
    private final String messageSource;
    private final Function<T, UUID> extractor;

    public DesignUpdateController(
            String messageSource,
            DesignEventStore eventStore,
            MessageEmitter<DesignAggregateUpdated> updateEmitter,
            MessageEmitter<TileRenderRequested> renderEmitter,
            Function<T, UUID> extractor
    ) {
        this.eventStore = Objects.requireNonNull(eventStore);
        this.messageSource = Objects.requireNonNull(messageSource);
        this.updateEmitter = Objects.requireNonNull(updateEmitter);
        this.renderEmitter = Objects.requireNonNull(renderEmitter);
        this.extractor = Objects.requireNonNull(extractor);
    }

    @Override
    public Single<Void> onNext(InputMessage<T> message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .map(receivedMessage -> receivedMessage.getValue().getData())
                .flatMapObservable(event -> onEventReceived(extractor.apply(event), message.getToken()))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(value -> null);
    }

    private Single<InputMessage<T>> onMessageReceived(InputMessage<T> message) {
        return eventStore.appendMessage(message).map(result -> message);
    }

    private Observable<Void> onEventReceived(UUID designId, String revision) {
        return updateDesign(designId, revision).flatMap(this::sendEvents);
    }

    private Observable<Design> updateDesign(UUID designId, String revision) {
        return eventStore.projectDesign(designId, revision)
                .flatMapObservable(result -> result.map(Observable::just).orElseGet(Observable::empty))
                .flatMapSingle(eventStore::updateDesign)
                .flatMap(result -> result.map(Observable::just).orElseGet(Observable::empty));
    }

    private Observable<Void> sendEvents(Design design) {
        return sendUpdateEvents(design).concatWith(sendRenderEvents(design));
    }

    private Observable<Void> sendUpdateEvents(Design design) {
        return createUpdateEvents(design)
                .map(this::createMessage)
                .flatMapSingle(updateEmitter::send);
    }

    private Observable<DesignAggregateUpdated> createUpdateEvents(Design design) {
        return Observable.just(createUpdateEvent(design));
    }

    private DesignAggregateUpdated createUpdateEvent(Design design) {
        return DesignAggregateUpdated.newBuilder()
                .setDesignId(design.getDesignId())
                .setCommandId(design.getCommandId())
                .setUserId(design.getUserId())
                .setRevision(design.getRevision())
                .setChecksum(design.getChecksum())
                .setData(design.getData())
                .setStatus(DesignAggregateStatus.valueOf(design.getStatus()))
                .setPublished(design.isPublished())
                .setLevels(design.getLevels())
                .setBitmap(design.getBitmap())
                .setCreated(design.getCreated().toInstant(ZoneOffset.UTC))
                .setUpdated(design.getUpdated().toInstant(ZoneOffset.UTC))
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
        return renderEmitter.send(createMessage(event), Render.getTopicName(renderEmitter.getTopicName() + "-requested", 0));
    }

    private Observable<TileRenderRequested> createRenderEvents(Design design) {
        return generateTiles(0)
                .concatWith(generateTiles(1))
                .concatWith(generateTiles(2))
                .map(tile -> createRenderEvent(design, tile));
    }

    private TileRenderRequested createRenderEvent(Design design, Tile tile) {
        return TileRenderRequested.newBuilder()
                .setDesignId(design.getDesignId())
                .setCommandId(design.getCommandId())
                .setRevision(design.getRevision())
                .setChecksum(design.getChecksum())
                .setData(design.getData())
                .setLevel(tile.getLevel())
                .setRow(tile.getRow())
                .setCol(tile.getCol())
                .build();
    }

    private OutputMessage<DesignAggregateUpdated> createMessage(DesignAggregateUpdated event) {
        return MessageFactory.<DesignAggregateUpdated>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }

    private OutputMessage<TileRenderRequested> createMessage(TileRenderRequested event) {
        return MessageFactory.<TileRenderRequested>of(messageSource)
                .createOutputMessage(Render.createRenderKey(event), event);
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
                String messageSource,
                DesignEventStore eventStore,
                MessageEmitter<DesignAggregateUpdated> eventsEmitter,
                MessageEmitter<TileRenderRequested> renderEmitter
        ) {
            super(messageSource, eventStore, eventsEmitter, renderEmitter, DesignInsertRequested::getDesignId);
        }
    }

    public static class DesignUpdateRequestedController extends DesignUpdateController<DesignUpdateRequested> {
        public DesignUpdateRequestedController(
                String messageSource,
                DesignEventStore eventStore,
                MessageEmitter<DesignAggregateUpdated> eventsEmitter,
                MessageEmitter<TileRenderRequested> renderEmitter
        ) {
            super(messageSource, eventStore, eventsEmitter, renderEmitter, DesignUpdateRequested::getDesignId);
        }
    }

    public static class DesignDeleteRequestedController extends DesignUpdateController<DesignDeleteRequested> {
        public DesignDeleteRequestedController(
                String messageSource,
                DesignEventStore eventStore,
                MessageEmitter<DesignAggregateUpdated> eventsEmitter,
                MessageEmitter<TileRenderRequested> renderEmitter
        ) {
            super(messageSource, eventStore, eventsEmitter, renderEmitter, DesignDeleteRequested::getDesignId);
        }
    }
}
