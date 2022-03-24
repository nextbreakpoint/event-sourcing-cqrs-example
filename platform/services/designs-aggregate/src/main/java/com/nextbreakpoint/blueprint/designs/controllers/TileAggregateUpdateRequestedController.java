package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.Objects;
import java.util.Optional;

public class TileAggregateUpdateRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileAggregateUpdateRequested> inputMapper;
    private final MessageMapper<TileAggregateUpdateCompleted, OutputMessage> updateOutputMapper;
    private final MessageEmitter emitter;
    private final DesignAggregate aggregate;

    public TileAggregateUpdateRequestedController(DesignAggregate aggregate, Mapper<InputMessage, TileAggregateUpdateRequested> inputMapper, MessageMapper<TileAggregateUpdateCompleted, OutputMessage> updateOutputMapper, MessageEmitter emitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper =  Objects.requireNonNull(inputMapper);
        this.updateOutputMapper = Objects.requireNonNull(updateOutputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onAggregateUpdateRequested)
                .map(updateOutputMapper::transform)
                .flatMapSingle(emitter::send)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(value -> null);
    }

    private Observable<TileAggregateUpdateCompleted> onAggregateUpdateRequested(TileAggregateUpdateRequested event) {
        return aggregate.projectDesign(event.getDesignId(), event.getRevision())
                .flatMap(result -> result.map(aggregate::updateDesign).orElseGet(() -> Single.just(Optional.empty())))
                .flatMapObservable(result -> result.map(design -> Observable.just(createEvent(design))).orElseGet(Observable::empty));
    }

    private TileAggregateUpdateCompleted createEvent(Design design) {
        return TileAggregateUpdateCompleted.builder()
                .withDesignId(design.getDesignId())
                .withRevision(design.getRevision())
                .build();
    }
}
