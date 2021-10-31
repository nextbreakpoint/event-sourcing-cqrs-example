package com.nextbreakpoint.blueprint.designs.operations;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndEvent;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.model.*;
import com.nextbreakpoint.blueprint.designs.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.designs.events.TileRenderUpdated;
import rx.Single;

import java.util.Objects;

public class TileRenderCompletedController implements Controller<RecordAndEvent<TileRenderCompleted>, Void> {
    private final Mapper<TileRenderUpdated, Message> mapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public TileRenderCompletedController(Store store, Mapper<TileRenderUpdated, Message> mapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.mapper = Objects.requireNonNull(mapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(RecordAndEvent<TileRenderCompleted> object) {
        return Single.just(new EventMetadata(object.getRecord(), Uuids.timeBased()))
                .flatMap(metadata -> updateTile(object.getEvent()))
                .map(mapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<TileRenderUpdated> updateTile(TileRenderCompleted event) {
        return store.updateDesignTile(new DesignTile(event.getChecksum(), event.getLevel(), event.getX(), event.getY()), event.getStatus())
                .map(result -> new TileRenderUpdated(event.getChecksum(), event.getLevel(), event.getX(), event.getY(), event.getStatus()));
    }
}