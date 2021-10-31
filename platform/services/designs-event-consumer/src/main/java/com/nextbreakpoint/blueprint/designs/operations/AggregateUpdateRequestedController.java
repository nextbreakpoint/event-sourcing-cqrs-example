package com.nextbreakpoint.blueprint.designs.operations;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndEvent;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.model.DesignChange;
import com.nextbreakpoint.blueprint.designs.events.AggregateUpdateRequested;
import com.nextbreakpoint.blueprint.designs.events.AggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.designs.model.EventMetadata;
import rx.Single;

import java.util.Date;
import java.util.Objects;

public class AggregateUpdateRequestedController implements Controller<RecordAndEvent<AggregateUpdateRequested>, Void> {
    private final Mapper<AggregateUpdateCompleted, Message> mapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public AggregateUpdateRequestedController(Store store, Mapper<AggregateUpdateCompleted, Message> mapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.mapper = Objects.requireNonNull(mapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(RecordAndEvent<AggregateUpdateRequested> object) {
        return Single.just(new EventMetadata(object.getRecord(), Uuids.timeBased()))
                .flatMap(metadata -> updateAggregate(object.getEvent()))
                .map(mapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<AggregateUpdateCompleted> updateAggregate(AggregateUpdateRequested event) {
        return store.updateDesignAggregate(event.getUuid())
                .map(result -> result.orElse(createEmptyChange(event)))
                .map(change -> new AggregateUpdateCompleted(change.getUuid(), change.getJson(), change.getChecksum()));
    }

    private DesignChange createEmptyChange(AggregateUpdateRequested event) {
        return new DesignChange(event.getUuid(), null, "DELETED", null, new Date(event.getTimestamp()));
    }
}
