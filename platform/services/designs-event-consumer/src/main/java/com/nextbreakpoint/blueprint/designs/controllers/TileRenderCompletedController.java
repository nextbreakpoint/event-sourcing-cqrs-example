package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;
import java.util.UUID;

public class TileRenderCompletedController implements Controller<Message, Void> {
    private final Mapper<Message, TileRenderCompleted> inputMapper;
    private final Mapper<TileAggregateUpdateRequired, Message> outputMapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public TileRenderCompletedController(Store store, Mapper<Message, TileRenderCompleted> inputMapper, Mapper<TileAggregateUpdateRequired, Message> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .flatMap(emitter::onNext);
    }

    private Single<Message> onMessageReceived(Message message) {
        final UUID esid = Uuids.timeBased();
        return store.appendMessage(esid, message)
                .map(result -> inputMapper.transform(message))
                .map(event -> new TileAggregateUpdateRequired(Uuids.timeBased(), event.getUuid(), esid))
                .map(outputMapper::transform);

    }
}
