package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;

public class TileRenderCompletedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileRenderCompleted> inputMapper;
    private final Mapper<TileAggregateUpdateRequired, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public TileRenderCompletedController(Store store, Mapper<InputMessage, TileRenderCompleted> inputMapper, Mapper<TileAggregateUpdateRequired, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message).flatMap(this::onMessageReceived);
    }

    private Single<Void> onMessageReceived(InputMessage message) {
        return store.appendMessage(message)
                .map(result -> inputMapper.transform(message))
                .map(event -> new TileAggregateUpdateRequired(Uuids.timeBased(), event.getUuid(), message.getOffset()))
                .map(outputMapper::transform)
                .flatMap(emitter::onNext);
    }
}
