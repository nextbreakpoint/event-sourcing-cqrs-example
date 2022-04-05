package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateTilesUpdateCompleted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignAggregateTilesUpdateCompletedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAggregateTilesUpdateCompleted> inputMapper;
    private final MessageMapper<DesignDocumentUpdateRequested, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final DesignAggregate aggregate;

    public DesignAggregateTilesUpdateCompletedController(DesignAggregate aggregate, Mapper<InputMessage, DesignAggregateTilesUpdateCompleted> inputMapper, MessageMapper<DesignDocumentUpdateRequested, OutputMessage> outputMapper, MessageEmitter emitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onAggregateUpdateCompleted)
                .map(outputMapper::transform)
                .flatMapSingle(emitter::send)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<DesignDocumentUpdateRequested> onAggregateUpdateCompleted(DesignAggregateTilesUpdateCompleted event) {
        return aggregate.findDesign(event.getDesignId())
                .flatMapObservable(result -> result.map(design -> Observable.just(createEvent(design))).orElseGet(Observable::empty));
    }

    private DesignDocumentUpdateRequested createEvent(Design design) {
        final TilesBitmap bitmap = TilesBitmap.of(design.getBitmap());

        final List<Tiles> tiles = IntStream.range(0, 8)
                .mapToObj(bitmap::toTiles)
                .collect(Collectors.toList());

        return DesignDocumentUpdateRequested.builder()
                .withUserId(design.getUserId())
                .withDesignId(design.getDesignId())
                .withCommandId(design.getCommandId())
                .withRevision(design.getRevision())
                .withData(design.getData())
                .withChecksum(design.getChecksum())
                .withStatus(design.getStatus())
                .withPublished(design.isPublished())
                .withLevels(design.getLevels())
                .withTiles(tiles)
                .withCreated(design.getCreated())
                .withUpdated(design.getUpdated())
                .build();
    }
}
