package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.AggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;
import java.util.UUID;

public class MessageAppendController implements Controller<Message, Void> {
    private final Mapper<AggregateUpdateRequested, Message> mapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public MessageAppendController(Store store, Mapper<AggregateUpdateRequested, Message> mapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.mapper = Objects.requireNonNull(mapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .flatMap(this::onEventReceived)
                .map(mapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<AggregateUpdateRequested> onEventReceived(Message message) {
        return store.appendMessage(Uuids.timeBased(), message)
                .map(result -> new AggregateUpdateRequested(UUID.fromString(message.getPartitionKey()), message.getTimestamp()));
    }
}
