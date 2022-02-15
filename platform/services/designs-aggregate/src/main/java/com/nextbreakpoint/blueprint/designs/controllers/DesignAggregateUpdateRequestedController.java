package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Single;

import java.util.Objects;

public class DesignAggregateUpdateRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignAggregateUpdateRequested> inputMapper;
    private final MessageMapper<DesignAggregateUpdateCompleted, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private final DesignAggregate aggregate;

    public DesignAggregateUpdateRequestedController(DesignAggregate aggregate, Mapper<InputMessage, DesignAggregateUpdateRequested> inputMapper, MessageMapper<DesignAggregateUpdateCompleted, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper =  Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMap(this::onAggregateUpdateRequested)
                .map(event -> outputMapper.transform(Tracing.from(message.getTrace()), event))
                .flatMap(emitter::onNext);
    }

    private Single<DesignAggregateUpdateCompleted> onAggregateUpdateRequested(DesignAggregateUpdateRequested event) {
        return aggregate.updateDesign(event.getDesignId(), event.getRevision())
                .map(result -> result.orElseThrow(() -> new RuntimeException("Design aggregate not found " + event.getDesignId())))
                .map(this::createEvent);
    }

    private DesignAggregateUpdateCompleted createEvent(Design design) {
        return DesignAggregateUpdateCompleted.builder()
                .withEventId(Uuids.timeBased())
                .withDesignId(design.getDesignId())
                .withRevision(design.getRevision())
                .withData(design.getData())
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
