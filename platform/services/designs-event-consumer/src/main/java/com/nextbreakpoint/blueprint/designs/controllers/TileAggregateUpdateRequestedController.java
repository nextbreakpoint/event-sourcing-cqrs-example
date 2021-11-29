package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;

public class TileAggregateUpdateRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileAggregateUpdateRequested> inputMapper;
    private final Mapper<TileAggregateUpdateCompleted, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public TileAggregateUpdateRequestedController(Store store, Mapper<InputMessage, TileAggregateUpdateRequested> inputMapper, Mapper<TileAggregateUpdateCompleted, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
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
//                .toCompletable()
//                .toSingleDefault("")
//                .map(result -> null);
    }

    private Single<TileAggregateUpdateCompleted> onAggregateUpdateRequested(TileAggregateUpdateRequested event) {
        return store.updateDesign(event.getUuid(), event.getEsid())
                .map(result -> result.orElseThrow(() -> new RuntimeException("Design aggregate not found " + event.getUuid())))
//                .flatMapObservable(result -> Observable.from(result.map(Collections::singletonList).orElseGet(Collections::emptyList)))
                .map(design -> new TileAggregateUpdateCompleted(Uuids.timeBased(), design.getUuid(), design.getEsid()));
    }
}
