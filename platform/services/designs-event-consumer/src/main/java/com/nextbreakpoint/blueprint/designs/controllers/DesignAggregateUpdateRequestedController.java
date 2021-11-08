package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Single;

import java.util.Objects;

public class DesignAggregateUpdateRequestedController implements Controller<Message, Void> {
    private final Logger logger = LoggerFactory.getLogger(DesignAggregateUpdateRequestedController.class.getName());

    private final Mapper<Message, DesignAggregateUpdateRequested> inputMapper;
    private final Mapper<DesignAggregateUpdateCompleted, Message> outputMapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public DesignAggregateUpdateRequestedController(Store store, Mapper<Message, DesignAggregateUpdateRequested> inputMapper, Mapper<DesignAggregateUpdateCompleted, Message> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper =  Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMap(this::onAggregateUpdateRequested)
                .map(outputMapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<DesignAggregateUpdateCompleted> onAggregateUpdateRequested(DesignAggregateUpdateRequested event) {
        return store.updateDesign(event.getUuid(), event.getEvid())
                .map(result -> result.orElseThrow(() -> new RuntimeException("Design aggregate not found " + event.getUuid())))
//                .flatMapObservable(result -> Observable.from(result.map(Collections::singletonList).orElseGet(Collections::emptyList)))
                .map(design -> new DesignAggregateUpdateCompleted(design.getUuid(), design.getUpdated().getTime(), design.getEvid(), design.getJson(), design.getChecksum(), design.getStatus()));
    }
}
