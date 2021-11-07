package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Observable;
import rx.Single;

import java.util.Collections;
import java.util.Objects;

public class TileAggregateUpdateRequestedController implements Controller<Message, Void> {
    private final Mapper<Message, TileAggregateUpdateRequested> inputMapper;
    private final Mapper<TileAggregateUpdateCompleted, Message> outputMapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public TileAggregateUpdateRequestedController(Store store, Mapper<Message, TileAggregateUpdateRequested> inputMapper, Mapper<TileAggregateUpdateCompleted, Message> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper =  Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onAggregateUpdateRequested)
                .map(outputMapper::transform)
                .flatMapSingle(emitter::onNext)
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<TileAggregateUpdateCompleted> onAggregateUpdateRequested(TileAggregateUpdateRequested event) {
        return store.updateDesign(event.getUuid())
//                .map(result -> result.orElseThrow(() -> new RuntimeException("Design aggregate not found " + event.getUuid())))
                .flatMapObservable(result -> Observable.from(result.map(Collections::singletonList).orElseGet(Collections::emptyList)))
                .map(design -> new TileAggregateUpdateCompleted(design.getUuid(), event.getTimestamp()));
    }
}
