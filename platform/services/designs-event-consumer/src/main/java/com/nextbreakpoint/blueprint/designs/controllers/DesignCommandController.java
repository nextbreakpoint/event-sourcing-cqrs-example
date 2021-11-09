package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import rx.Single;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class DesignCommandController implements Controller<Message, Void> {
    private final Mapper<DesignAggregateUpdateRequested, Message> mapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public DesignCommandController(Store store, Mapper<DesignAggregateUpdateRequested, Message> mapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.mapper = Objects.requireNonNull(mapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .map(mapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<DesignAggregateUpdateRequested> onMessageReceived(Message message) {
        final UUID esid = Uuids.timeBased();
        return store.appendMessage(esid, message)
                .map(result -> new DesignAggregateUpdateRequested(Uuids.timeBased(), UUID.fromString(message.getPartitionKey()), esid));
    }
}
