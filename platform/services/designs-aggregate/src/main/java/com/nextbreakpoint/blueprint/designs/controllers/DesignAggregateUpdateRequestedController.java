package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.Objects;
import java.util.Optional;

public class DesignAggregateUpdateRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAggregateUpdateRequested> inputMapper;
    private final MessageMapper<DesignAggregateUpdateCompleted, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final DesignAggregate aggregate;

    public DesignAggregateUpdateRequestedController(DesignAggregate aggregate, Mapper<InputMessage, DesignAggregateUpdateRequested> inputMapper, MessageMapper<DesignAggregateUpdateCompleted, OutputMessage> outputMapper, MessageEmitter emitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper =  Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onAggregateUpdateRequested)
                .map(outputMapper::transform)
                .flatMapSingle(emitter::send)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(value -> null);
    }


    private Observable<DesignAggregateUpdateCompleted> onAggregateUpdateRequested(DesignAggregateUpdateRequested event) {
        return aggregate.projectDesign(event.getDesignId(), event.getRevision())
                .flatMap(result -> result.map(aggregate::updateDesign).orElseGet(() -> Single.just(Optional.empty())))
                .flatMapObservable(result -> result.map(design -> Observable.just(createEvent(design))).orElseGet(Observable::empty));
    }

    private DesignAggregateUpdateCompleted createEvent(Design design) {
        return DesignAggregateUpdateCompleted.builder()
                .withDesignId(design.getDesignId())
                .withRevision(design.getRevision())
                .withData(design.getData())
                .withChecksum(design.getChecksum())
                .withLevels(design.getLevels())
                .withStatus(design.getStatus())
                .build();
    }
}
