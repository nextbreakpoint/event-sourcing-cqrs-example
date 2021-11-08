package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;

public class TileAggregateUpdateRequiredController implements Controller<Message, Void> {
    private final Mapper<Message, TileAggregateUpdateRequired> inputMapper;
    private final Mapper<TileAggregateUpdateRequested, Message> outputMapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public TileAggregateUpdateRequiredController(Store store, Mapper<Message, TileAggregateUpdateRequired> inputMapper, Mapper<TileAggregateUpdateRequested, Message> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper =  Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMap(this::onAggregateUpdateRequired)
                .map(outputMapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<TileAggregateUpdateRequested> onAggregateUpdateRequired(TileAggregateUpdateRequired event) {
        return Single.just(new TileAggregateUpdateRequested(event.getUuid(), event.getEvid(), event.getTimestamp()));
    }
}
