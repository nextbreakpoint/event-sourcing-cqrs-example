package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregateManager;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Single;

import java.util.Objects;

public class DesignAggregateUpdateRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAggregateUpdateRequested> inputMapper;
    private final Mapper<DesignAggregateUpdateCompleted, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private final DesignAggregateManager aggregateManager;

    public DesignAggregateUpdateRequestedController(DesignAggregateManager aggregateManager, Mapper<InputMessage, DesignAggregateUpdateRequested> inputMapper, Mapper<DesignAggregateUpdateCompleted, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.aggregateManager = Objects.requireNonNull(aggregateManager);
        this.inputMapper =  Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMap(this::onAggregateUpdateRequested)
                .map(outputMapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<DesignAggregateUpdateCompleted> onAggregateUpdateRequested(DesignAggregateUpdateRequested event) {
        return aggregateManager.updateDesign(event.getUuid(), event.getEsid())
                .map(result -> result.orElseThrow(() -> new RuntimeException("Design aggregate not found " + event.getUuid())))
                .map(this::createEvent);
    }

    private DesignAggregateUpdateCompleted createEvent(Design design) {
        return DesignAggregateUpdateCompleted.builder()
                .withEvid(Uuids.timeBased())
                .withUuid(design.getUuid())
                .withEsid(design.getEsid())
                .withData(design.getJson())
                .withChecksum(design.getChecksum())
                .withLevels(design.getLevels())
                .withStatus(design.getStatus())
                .build();
    }

//    @Override
//    public Single<Void> onNext(Message message) {
//        return Single.just(message)
//                .map(inputMapper::transform)
//                .flatMapObservable(this::onAggregateUpdateRequested)
//                .map(outputMapper::transform)
//                .flatMapSingle(emitter::onNext)
//                .ignoreElements()
//                .toCompletable()
//                .toSingleDefault("")
//                .map(result -> null);
//    }
//
//    private Observable<DesignAggregateUpdateCompleted> onAggregateUpdateRequested(DesignAggregateUpdateRequested event) {
//        return store.updateDesign(event.getUuid(), event.getEsid())
////                .map(result -> result.orElseThrow(() -> new RuntimeException("Design aggregate not found " + event.getUuid())))
//                .flatMapObservable(result -> Observable.from(result.map(Collections::singletonList).orElseGet(Collections::emptyList)))
//                .map(this::createEvent);
//    }
}
