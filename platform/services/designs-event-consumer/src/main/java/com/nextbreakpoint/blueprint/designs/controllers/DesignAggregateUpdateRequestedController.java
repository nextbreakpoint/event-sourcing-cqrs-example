package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.AggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.AggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Single;

import java.util.Objects;

public class DesignAggregateUpdateRequestedController implements Controller<Message, Void> {
    private final Logger logger = LoggerFactory.getLogger(DesignAggregateUpdateRequestedController.class.getName());

    private final Mapper<Message, AggregateUpdateRequested> inputMapper;
    private final Mapper<AggregateUpdateCompleted, Message> outputMapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public DesignAggregateUpdateRequestedController(Store store, Mapper<Message, AggregateUpdateRequested> inputMapper, Mapper<AggregateUpdateCompleted, Message> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper =  Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMap(this::onEventReceived)
                .map(outputMapper::transform)
                .flatMap(emitter::onNext)
                .doOnError(err -> logger.error("Can't update design", err))
                .onErrorResumeNext(Single.just(null));
    }

    private Single<AggregateUpdateCompleted> onEventReceived(AggregateUpdateRequested event) {
        return store.updateDesign(event.getUuid())
                .map(result -> result.orElseThrow(() -> new RuntimeException("Design aggregate not found " + event.getUuid())))
                .map(design -> new AggregateUpdateCompleted(design.getUuid(), design.getEvid(), design.getJson(), design.getChecksum(), event.getStatus()));
    }
}
