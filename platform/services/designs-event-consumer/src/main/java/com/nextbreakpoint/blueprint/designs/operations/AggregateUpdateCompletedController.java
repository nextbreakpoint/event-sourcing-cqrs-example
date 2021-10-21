package com.nextbreakpoint.blueprint.designs.operations;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndEvent;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.model.DesignVersion;
import com.nextbreakpoint.blueprint.designs.model.EventMetadata;
import com.nextbreakpoint.blueprint.designs.events.AggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.designs.events.VersionCreated;
import rx.Single;

import java.util.Objects;

public class AggregateUpdateCompletedController<T extends RecordAndEvent<AggregateUpdateCompleted>> implements Controller<T, Void> {
    private final Mapper<VersionCreated, Message> mapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public AggregateUpdateCompletedController(Store store, Mapper<VersionCreated, Message> mapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.mapper = Objects.requireNonNull(mapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(T object) {
        return Single.fromCallable(() -> new EventMetadata(object.getRecord(), Uuids.timeBased()))
                .flatMap(metadata -> updateVersion(object.getEvent()))
                .map(mapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<VersionCreated> updateVersion(AggregateUpdateCompleted event) {
        return store.insertDesignVersion(new DesignVersion(event.getChecksum(), event.getData()))
                .map(result -> new VersionCreated(event.getChecksum(), event.getData()));
    }
}
