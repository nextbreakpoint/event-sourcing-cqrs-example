package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateTilesUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateTilesUpdateRequested;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.Objects;
import java.util.Optional;

public class DesignAggregateTilesUpdateRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAggregateTilesUpdateRequested> inputMapper;
    private final MessageMapper<DesignAggregateTilesUpdateCompleted, OutputMessage> updateOutputMapper;
    private final MessageEmitter emitter;
    private final DesignAggregate aggregate;

    public DesignAggregateTilesUpdateRequestedController(DesignAggregate aggregate, Mapper<InputMessage, DesignAggregateTilesUpdateRequested> inputMapper, MessageMapper<DesignAggregateTilesUpdateCompleted, OutputMessage> updateOutputMapper, MessageEmitter emitter) {
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

    private Observable<DesignAggregateTilesUpdateCompleted> onAggregateUpdateRequested(DesignAggregateTilesUpdateRequested event) {
        return aggregate.projectDesign(event.getDesignId(), event.getRevision())
                .flatMap(result -> result.map(aggregate::updateDesign).orElseGet(() -> Single.just(Optional.empty())))
                .flatMapObservable(result -> result.map(design -> Observable.just(createEvent(design))).orElseGet(Observable::empty));
    }

    private DesignAggregateTilesUpdateCompleted createEvent(Design design) {
        return DesignAggregateTilesUpdateCompleted.builder()
                .withDesignId(design.getDesignId())
                .withCommandId(design.getCommandId())
                .withRevision(design.getRevision())
                .build();
    }
}
