package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Single;

import java.util.Objects;

public class TileAggregateUpdateRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileAggregateUpdateRequested> inputMapper;
    private final MessageMapper<TileAggregateUpdateCompleted, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private final DesignAggregate aggregate;

    public TileAggregateUpdateRequestedController(DesignAggregate aggregate, Mapper<InputMessage, TileAggregateUpdateRequested> inputMapper, MessageMapper<TileAggregateUpdateCompleted, OutputMessage> outputMapper, KafkaEmitter emitter) {
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
//                .toCompletable()
//                .toSingleDefault("")
//                .map(result -> null);
    }

    private Single<TileAggregateUpdateCompleted> onAggregateUpdateRequested(TileAggregateUpdateRequested event) {
        return aggregate.updateDesign(event.getDesignId(), event.getRevision())
                .map(result -> result.orElseThrow(() -> new RuntimeException("Design aggregate not found " + event.getDesignId())))
//                .flatMapObservable(result -> Observable.from(result.map(Collections::singletonList).orElseGet(Collections::emptyList)))
                .map(this::createEvent);
    }

    private TileAggregateUpdateCompleted createEvent(Design design) {
        return TileAggregateUpdateCompleted.builder()
                .withEventId(TimeUUID.next())
                .withDesignId(design.getDesignId())
                .withRevision(design.getRevision())
                .build();
    }
}
