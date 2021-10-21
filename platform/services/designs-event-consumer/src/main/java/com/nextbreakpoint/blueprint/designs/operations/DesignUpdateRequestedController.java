package com.nextbreakpoint.blueprint.designs.operations;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndEvent;
import com.nextbreakpoint.blueprint.designs.common.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.events.AggregateUpdateRequested;
import com.nextbreakpoint.blueprint.designs.model.GenericEvent;
import com.nextbreakpoint.blueprint.designs.model.EventMetadata;
import rx.Single;

import java.util.Objects;
import java.util.function.BiFunction;

public class DesignUpdateRequestedController<V extends GenericEvent, T extends RecordAndEvent<V>> implements Controller<T, Void> {
    private final Mapper<AggregateUpdateRequested, Message> mapper;
    private final BiFunction<EventMetadata, V, Single<AggregateUpdateRequested>> handler;
    private final KafkaEmitter emitter;

    public DesignUpdateRequestedController(
        BiFunction<EventMetadata, V, Single<AggregateUpdateRequested>> handler,
        Mapper<AggregateUpdateRequested, Message> mapper, KafkaEmitter emitter
    ) {
        this.handler = Objects.requireNonNull(handler);
        this.mapper = Objects.requireNonNull(mapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(T object) {
        return Single.fromCallable(() -> new EventMetadata(object.getRecord(), Uuids.timeBased()))
                .flatMap(metadata -> handler.apply(metadata, object.getEvent()))
                .map(mapper::transform)
                .flatMap(emitter::onNext);
    }
}
